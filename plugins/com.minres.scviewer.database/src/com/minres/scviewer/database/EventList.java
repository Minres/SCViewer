package com.minres.scviewer.database;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class EventList<K,V>  implements IEventList<K,V>{

	NavigableMap<K,V> backing ;

	public EventList() {
		backing=new TreeMap<>();
	}

	public EventList(NavigableMap<K, V> subMap) {
		backing=subMap;
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return backing.floorEntry(key);
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return backing.ceilingEntry(key);
	}

	@Override
	public Entry<K, V> firstEntry() {
		return  backing.firstEntry();
	}

	@Override
	public Entry<K, V> lastEntry() {
		return backing.lastEntry();
	}

	@Override
	public V get(K key) {
		return backing.get(key);
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return backing.higherEntry(key);
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return backing.lowerEntry(key);
	}

	@Override
	public IEventList<K, V> subMap(K key, boolean b, K key2, boolean c) {
		return new EventList<K,V>( backing.subMap(key, b, key2, c));
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public Collection<K> keys() {
		return backing.keySet();
	}

	@Override
	public Collection<V> values() {
		return backing.values();
	}

	@Override
	public boolean containsKey(K key) {
		return backing.containsKey(key);
	}

	@Override
	public V put(K key, V value) {
		return backing.put(key, value);
	}

	@Override
	public Collection<Entry<K, V>> entrySet() {
		return backing.entrySet();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public K lastKey() {
		return backing.lastKey();
	}

}
