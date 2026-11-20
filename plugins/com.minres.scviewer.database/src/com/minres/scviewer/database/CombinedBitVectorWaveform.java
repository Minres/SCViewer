/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/** An immutable composition of recorded bits, listed from MSB to LSB.
 * Recording event lists must be immutable after loading. Range views merge lazily;
 * only a bounded set of sampled values is cached, never the entire merged timeline.
 */
public final class CombinedBitVectorWaveform extends HierNode implements IWaveform {
    public record SourceBit(IWaveform waveform, int bit) {
        public SourceBit {
            if (waveform == null || waveform.getType() != WaveformType.SIGNAL
                    || bit < 0 || bit >= waveform.getWidth())
                throw new IllegalArgumentException("Invalid source bit");
        }
    }

    private final String identifier;
    private final List<SourceBit> sources;
    private final LazyValue<List<IEventList>> events = new LazyValue<>();
    private final Map<Long, BitValue[]> samples = new LinkedHashMap<>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, BitValue[]> entry) { return size() > 256; }
    };
    private final IEventList timeline = new Timeline(0L, true, -1L, false);
    private record Seek(long time, boolean forward, boolean inclusive, long limit) { }
    private final Map<Seek, Long> positions = new LinkedHashMap<>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Seek, Long> entry) { return size() > 256; }
    };

    public CombinedBitVectorWaveform(String name, List<SourceBit> sources) {
        this("@combined:" + UUID.randomUUID(), name, sources);
    }

    public CombinedBitVectorWaveform(String identifier, String name, List<SourceBit> sources) {
        super(name);
        if (identifier == null || !identifier.startsWith("@combined:") || identifier.length() <= 10
                || name == null || name.isBlank() || sources == null || sources.size() < 2)
            throw new IllegalArgumentException("A combined vector needs an identity, a name and at least two bits");
        this.identifier = identifier;
        List<SourceBit> normalized = new ArrayList<>();
        for (SourceBit input : sources) {
            while (input.waveform() instanceof CombinedBitVectorWaveform combined)
                input = combined.sources.get(combined.getWidth() - 1 - input.bit());
            normalized.add(input);
        }
        this.sources = List.copyOf(normalized);
    }

    public List<SourceBit> getSources() { return sources; }
    @Override public String getFullName() { return identifier; }
    @Override public String getDisplayName() { return getName(); }
    @Override public DirectionType getDirection() { return DirectionType.IMPLICIT; }
    @Override public long getId() { return identifier.hashCode(); }
    @Override public boolean isSame(IWaveform other) {
        return other instanceof CombinedBitVectorWaveform combined && identifier.equals(combined.identifier);
    }
    @Override public WaveformType getType() { return WaveformType.SIGNAL; }
    @Override public String getKind() { return "combined bitvector"; }
    @Override public int getWidth() { return sources.size(); }
    @Override public int getRowCount() { return 1; }
    @Override public IEventList getEvents() { return timeline; }
    @Override public IEvent[] getEventsAtTime(long time) { return timeline.get(time); }
    @Override public IEvent[] getEventsBeforeTime(long time) {
        EventEntry entry = timeline.floorEntry(time);
        return entry == null ? new IEvent[0] : entry.events;
    }

    private List<IEventList> inputs() {
        return events.get(() -> sources.stream().map(bit -> bit.waveform().getEvents()).toList());
    }

    private BitValue[] sample(long time) {
        synchronized (samples) {
            BitValue[] cached = samples.get(time);
            if (cached != null) return cached;
        }
        BitValue[] value = new BitValue[getWidth()];
        for (int i = 0; i < value.length; i++) {
            EventEntry entry = inputs().get(i).floorEntry(time);
            value[i] = BitValue.X;
            if (entry != null && entry.events != null && entry.events.length > 0) {
                IEvent event = entry.events[entry.events.length - 1];
                if (!(event instanceof BitVector vector))
                    throw new IllegalArgumentException("Source is not a bit signal: " + sources.get(i).waveform().getFullName());
                value[i] = vector.getValue(sources.get(i).bit());
            }
        }
        synchronized (samples) { samples.put(time, value); }
        return value;
    }

    /** Find the next/previous timestamp in the union, before suppressing unchanged samples. */
    private Long candidate(long time, boolean forward, boolean inclusive) {
        Long result = null;
        for (IEventList input : inputs()) {
            EventEntry entry = forward ? (inclusive ? input.ceilingEntry(time) : input.higherEntry(time))
                    : (inclusive ? input.floorEntry(time) : input.lowerEntry(time));
            if (entry != null && (result == null || (forward
                    ? Long.compareUnsigned(entry.timestamp, result) < 0
                    : Long.compareUnsigned(entry.timestamp, result) > 0))) result = entry.timestamp;
        }
        return result;
    }

    private boolean changed(long time) {
        Long previous = candidate(time, false, false);
        return previous == null || !Arrays.equals(sample(time), sample(previous));
    }

    private Long seek(long time, boolean forward, boolean inclusive, long limit) {
        Seek query = new Seek(time, forward, inclusive, limit);
        synchronized (positions) {
            if (positions.containsKey(query)) return positions.get(query);
        }
        Long next = candidate(time, forward, inclusive);
        while (next != null) {
            if (forward ? Long.compareUnsigned(next, limit) > 0 : Long.compareUnsigned(next, limit) < 0) {
                next = null;
                break;
            }
            if (changed(next)) break;
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            next = candidate(next, forward, false);
        }
        synchronized (positions) { positions.put(query, next); }
        return next;
    }

    private EventEntry event(long time) {
        BitVector vector = new BitVector(getWidth());
        BitValue[] value = sample(time);
        for (int i = 0; i < value.length; i++) vector.setValue(value.length - i - 1, value[i]);
        return new EventEntry(time, new IEvent[]{vector});
    }

    private final class Timeline implements IEventList {
        private final long low, high;
        private final boolean includeLow, empty;
        Timeline(long low, boolean includeLow, long high, boolean empty) {
            this.low = low; this.high = high; this.includeLow = includeLow; this.empty = empty;
        }
        private EventEntry find(long time, boolean forward, boolean inclusive) {
            if (empty) return null;
            if (forward && Long.compareUnsigned(time, low) < 0) { time = low; inclusive = includeLow; }
            else if (forward && time == low) inclusive &= includeLow;
            if (!forward && Long.compareUnsigned(time, high) > 0) { time = high; inclusive = true; }
            Long next = seek(time, forward, inclusive, forward ? high : low);
            return next == null || Long.compareUnsigned(next, low) < 0 || (!includeLow && next == low)
                    || Long.compareUnsigned(next, high) > 0 ? null : event(next);
        }
        @Override public EventEntry firstEntry() { return find(low, true, includeLow); }
        @Override public EventEntry lastEntry() { return find(high, false, true); }
        @Override public EventEntry floorEntry(long key) { return find(key, false, true); }
        @Override public EventEntry ceilingEntry(long key) { return find(key, true, true); }
        @Override public EventEntry lowerEntry(long key) { return find(key, false, false); }
        @Override public EventEntry higherEntry(long key) { return find(key, true, false); }
        @Override public boolean isEmpty() { return firstEntry() == null; }
        @Override public int size() { int size = 0; for (EventEntry ignored : this) size++; return size; }
        @Override public long firstKey() {
            EventEntry entry = firstEntry(); if (entry == null) throw new NoSuchElementException(); return entry.timestamp;
        }
        @Override public long lastKey() {
            EventEntry entry = lastEntry(); if (entry == null) throw new NoSuchElementException(); return entry.timestamp;
        }
        @Override public IEvent[] get(long key) {
            EventEntry entry = ceilingEntry(key); return entry != null && entry.timestamp == key ? entry.events : null;
        }
        @Override public boolean containsKey(long key) { return get(key) != null; }
        @Override public void put(long key, IEvent value) { throw new UnsupportedOperationException("Read-only combined vector"); }
        @Override public IEventList subMap(long from, boolean inclusive, long to) {
            if (Long.compareUnsigned(from, to) > 0) throw new IllegalArgumentException("Reversed time range");
            long lower = Long.compareUnsigned(from, low) > 0 ? from : low;
            boolean include = (lower != from || inclusive) && (lower != low || includeLow);
            long upper = Long.compareUnsigned(to, high) < 0 ? to : high;
            return new Timeline(lower, include, upper, empty || Long.compareUnsigned(lower, upper) > 0);
        }
        @Override public Iterator<EventEntry> iterator() {
            return new Iterator<>() {
                EventEntry next = firstEntry();
                @Override public boolean hasNext() { return next != null; }
                @Override public EventEntry next() {
                    if (next == null) throw new NoSuchElementException();
                    EventEntry result = next; next = higherEntry(result.timestamp); return result;
                }
            };
        }
        @Override public Collection<EventEntry> entrySet() {
            return new AbstractCollection<>() {
                @Override public int size() { return Timeline.this.size(); }
                @Override public Iterator<EventEntry> iterator() { return Timeline.this.iterator(); }
            };
        }
    }
}
