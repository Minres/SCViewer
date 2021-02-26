package com.minres.scviewer.database;

import java.util.Collection;
import java.util.Map.Entry;

public interface IEventList<K,V> {

	Entry<K, V> floorEntry(K key);

	Entry<K, V> ceilingEntry(K key);

	Entry<K, V> firstEntry();

	Entry<K, V> lastEntry();

	V get(K key);

	Entry<K, V> higherEntry(K key);

	Entry<K, V> lowerEntry(K key);

	IEventList<K, V> subMap(K key, boolean b, K key2, boolean c);

	int size();

	Collection<K> keys();
	
	Collection<V> values();

	boolean containsKey(K key);

	V put(K key, V value);

	Collection<Entry<K, V>> entrySet();

	boolean isEmpty();

	K lastKey();

}
