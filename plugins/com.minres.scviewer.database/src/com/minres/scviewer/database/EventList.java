package com.minres.scviewer.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

/** Sorted timestamp buckets. Range views are read-only and bounded by their parent. */
public class EventList implements IEventList {
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
