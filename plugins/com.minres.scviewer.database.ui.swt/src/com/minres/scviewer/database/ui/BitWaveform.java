/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DirectionType;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;

/** A read-only bit projection. Events are projected on demand, never copied in bulk. */
public final class BitWaveform extends HierNode implements IWaveform {

	private final IWaveform source;
	private final int bit;

	public BitWaveform(IWaveform source, int bit) {
		super("[" + bit + "]", source);
		if (source.getType() != WaveformType.SIGNAL || bit < 0 || bit >= source.getWidth())
			throw new IllegalArgumentException("Invalid signal bit: " + bit);
		this.source = source;
		this.bit = bit;
	}

	@Override public String getFullName() { return source.getFullName() + getName(); }
	@Override public DirectionType getDirection() { return source.getDirection(); }
	@Override public long getId() { return source.getId(); }
	@Override public WaveformType getType() { return WaveformType.SIGNAL; }
	@Override public String getKind() { return "signal"; }
	@Override public int getRowCount() { return 1; }
	@Override public int getWidth() { return 1; }
	@Override public boolean isSame(IWaveform other) {
		return other instanceof BitWaveform value && bit == value.bit
				&& (source == value.source || source.isSame(value.source));
	}
	@Override public IEventList getEvents() { return new BitEvents(source.getEvents()); }
	@Override public IEvent[] getEventsAtTime(long time) { return project(source.getEventsAtTime(time)); }
	@Override public IEvent[] getEventsBeforeTime(long time) { return project(source.getEventsBeforeTime(time)); }

	private IEvent[] project(IEvent[] events) {
		if (events == null) return null;
		IEvent[] result = new IEvent[events.length];
		for (int i = 0; i < events.length; i++) {
			BitVector value = new BitVector(1);
			value.setValue(0, ((BitVector) events[i]).getValue(bit));
			result[i] = value;
		}
		return result;
	}

	private EventEntry project(EventEntry entry) {
		return entry == null ? null : new EventEntry(entry.timestamp, project(entry.events));
	}

	/** Retains the source timeline and unsigned range semantics, including empty ranges. */
	private final class BitEvents implements IEventList {
		private final IEventList events;
		BitEvents(IEventList events) { this.events = events; }
		@Override public int size() { return events.size(); }
		@Override public boolean isEmpty() { return events.isEmpty(); }
		@Override public boolean containsKey(long key) { return events.containsKey(key); }
		@Override public long firstKey() { return events.firstKey(); }
		@Override public long lastKey() { return events.lastKey(); }
		@Override public IEvent[] get(long key) { return project(events.get(key)); }
		@Override public EventEntry firstEntry() { return project(events.firstEntry()); }
		@Override public EventEntry lastEntry() { return project(events.lastEntry()); }
		@Override public EventEntry floorEntry(long key) { return project(events.floorEntry(key)); }
		@Override public EventEntry ceilingEntry(long key) { return project(events.ceilingEntry(key)); }
		@Override public EventEntry higherEntry(long key) { return project(events.higherEntry(key)); }
		@Override public EventEntry lowerEntry(long key) { return project(events.lowerEntry(key)); }
		@Override public IEventList subMap(long from, boolean inclusive, long to) {
			return new BitEvents(events.subMap(from, inclusive, to));
		}
		@Override public void put(long key, IEvent value) { throw new UnsupportedOperationException("Read-only bit view"); }
		@Override public Iterator<EventEntry> iterator() {
			Iterator<EventEntry> iterator = events.iterator();
			return new Iterator<>() {
				@Override public boolean hasNext() { return iterator.hasNext(); }
				@Override public EventEntry next() { return project(iterator.next()); }
			};
		}
		@Override public Collection<EventEntry> entrySet() {
			return new AbstractCollection<>() {
				@Override public int size() { return events.size(); }
				@Override public Iterator<EventEntry> iterator() { return BitEvents.this.iterator(); }
			};
		}
	}
}
