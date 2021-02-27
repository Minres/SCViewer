package com.minres.scviewer.database;

import java.util.Collection;

public interface IEventList extends Iterable<EventEntry> {

	int size();

	Collection<EventEntry> entrySet();

	boolean containsKey(long key);

	IEvent[] get(long key);

	IEvent[] put(long key, IEvent[] value);

	long firstKey();

	long lastKey();

	boolean isEmpty();
	/**
	 * Returns a key-value mapping associated with the greatest key less 
	 * than or equal to the given key, or null if there is no such key.
	 * 
	 * @param key
	 * @return
	 */
	EventEntry floorEntry(long key);
	/**
	 * Returns a key-value mapping associated with the least key greater 
	 * than or equal to the given key, or null if there is no such key.
	 * 
	 * @param key
	 * @return
	 */
	EventEntry ceilingEntry(long key);
	/**
	 * Returns a key-value mapping associated with the least key in this map,
	 * or null if the map is empty.
	 * 
	 * @return
	 */
	EventEntry firstEntry();
	/**
	 * Returns a key-value mapping associated with the least key in this map,
	 * or null if the map is empty.
	 * 
	 * @return
	 */
	EventEntry lastEntry();
	/**
	 * Returns a key-value mapping associated with the least key strictly greater
	 * than the given key, or null if there is no such key.
	 * 
	 * @param key
	 * @return
	 */
	EventEntry higherEntry(long key);
	/**
	 * Returns a key-value mapping associated with the greatest key strictly less 
	 * than the given key, or null if there is no such key.
	 * 
	 * @param key
	 * @return
	 */
	EventEntry lowerEntry(long key);
    /**
     * Returns a view of the portion of this map whose keys range from
     * {@code fromKey} to {@code toKey}.  If {@code fromKey} and
     * {@code toKey} are equal, the returned map is empty unless
     * {@code fromInclusive} is true.  The
     * returned map is backed by this map, so changes in the returned map are
     * reflected in this map, and vice-versa.  The returned map supports all
     * optional map operations that this map supports.
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param fromInclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     * @param toKey high endpoint of the keys in the returned map (inclusive)
     */
	IEventList subMap(long key, boolean b, long key2);
	
}
