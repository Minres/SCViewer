/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

/** Sorted timestamp buckets. Range views are read-only and bounded by their parent. */
public class EventList implements IEventList {
    /** Accumulates unsorted events and freezes timestamp buckets with one sort and one allocation per bucket. */
    public static final class Builder {
        private record TimedEvent(long time, IEvent event) { }
        private final ArrayList<TimedEvent> pending = new ArrayList<>();
        private boolean built;

        public void add(long time, IEvent event) {
            if (built) throw new IllegalStateException("Event list has already been built");
            pending.add(new TimedEvent(time, event));
        }

        public EventList build() {
            if (built) throw new IllegalStateException("Event list has already been built");
            pending.sort((a, b) -> Long.compareUnsigned(a.time(), b.time()));
            ArrayList<EventEntry> entries = new ArrayList<>();
            for (int first = 0; first < pending.size();) {
                long time = pending.get(first).time();
                int next = first + 1;
                while (next < pending.size() && pending.get(next).time() == time) next++;
                IEvent[] events = new IEvent[next - first];
                for (int i = first; i < next; ++i) events[i - first] = pending.get(i).event();
                entries.add(new EventEntry(time, events));
                first = next;
            }
            built = true;
            pending.clear();
            return new EventList(entries, 0, entries.size(), true);
        }
    }

    private final ArrayList<EventEntry> store;
    private final int start;
    private int end;
    private final boolean unmodifiable;

    public EventList() {
        this(new ArrayList<>(), 0, 0, false);
    }

    private EventList(ArrayList<EventEntry> store, int start, int end, boolean unmodifiable) {
        this.store = store;
        this.start = start;
        this.end = end;
        this.unmodifiable = unmodifiable;
    }

    /** Index of the first timestamp >= key (or > key when exclusive). */
    private int bound(long key, boolean exclusive) {
        int low = start, high = end;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int cmp = Long.compareUnsigned(store.get(mid).timestamp, key);
            if (cmp < 0 || (exclusive && cmp == 0)) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private EventEntry entry(int index) {
        return index < start || index >= end ? null : store.get(index);
    }

    @Override
    public IEventList subMap(long from, boolean includeFrom, long to) {
        if (Long.compareUnsigned(from, to) > 0) throw new IllegalArgumentException("Reversed time range");
        int first = bound(from, !includeFrom);
        return new EventList(store, first, Math.max(first, bound(to, true)), true);
    }

    @Override
    public int size() { return end - start; }

    @Override
    public boolean containsKey(long key) {
        EventEntry e = entry(bound(key, false));
        return e != null && e.timestamp == key;
    }

    @Override
    public IEvent[] get(long key) {
        EventEntry e = entry(bound(key, false));
        return e != null && e.timestamp == key ? e.events : null;
    }

    @Override
    public void put(long key, IEvent value) {
        if (unmodifiable) throw new UnsupportedOperationException();
        int index = store.isEmpty() || Long.compareUnsigned(store.get(end - 1).timestamp, key) < 0
                ? end : bound(key, false);
        if (index < end && store.get(index).timestamp == key) store.get(index).append(value);
        else store.add(index, new EventEntry(key, new IEvent[] {value}));
        end = store.size();
    }

    @Override
    public Collection<EventEntry> entrySet() {
        return Collections.unmodifiableList(store.subList(start, end));
    }

    @Override
    public boolean isEmpty() { return start == end; }

    @Override
    public long firstKey() {
        if (isEmpty()) throw new NoSuchElementException();
        return store.get(start).timestamp;
    }

    @Override
    public long lastKey() {
        if (isEmpty()) throw new NoSuchElementException();
        return store.get(end - 1).timestamp;
    }

    @Override
    public EventEntry firstEntry() { return entry(start); }

    @Override
    public EventEntry lastEntry() { return entry(end - 1); }

    @Override
    public EventEntry floorEntry(long key) { return entry(bound(key, true) - 1); }

    @Override
    public EventEntry ceilingEntry(long key) { return entry(bound(key, false)); }

    @Override
    public EventEntry lowerEntry(long key) { return entry(bound(key, false) - 1); }

    @Override
    public EventEntry higherEntry(long key) { return entry(bound(key, true)); }

    @Override
    public java.util.Iterator<EventEntry> iterator() { return entrySet().iterator(); }
}
