/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.util.Collection;

public interface IEventList extends Iterable<EventEntry> {

	int size();

	Collection<EventEntry> entrySet();

	boolean containsKey(long key);

	IEvent[] get(long key);

	void put(long key, IEvent value);

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
     * returned view is read-only. Do not structurally modify a mutable source
     * while a view is in use. EventList.Builder produces frozen sources suitable
     * for sharing range views.
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param fromInclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     * @param toKey high endpoint of the keys in the returned map (inclusive)
     */
	IEventList subMap(long key, boolean b, long key2);
	
}
