package com.minres.scviewer.database.leveldb;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.iq80.leveldb.shaded.guava.collect.Maps.immutableEntry;

import java.util.Map.Entry;

import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.impl.SeekingIterator;

class StringDbIterator implements SeekingIterator<String, String> {
	private final DBIterator iterator;

	StringDbIterator(DBIterator iterator) {
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public void seekToFirst() {
		iterator.seekToFirst();
	}

	@Override
	public void seek(String targetKey) {
		iterator.seek(targetKey.getBytes(UTF_8));
	}

	@Override
	public Entry<String, String> peek() {
		return adapt(iterator.peekNext());
	}

	@Override
	public Entry<String, String> next() {
		return adapt(iterator.next());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private Entry<String, String> adapt(Entry<byte[], byte[]> next) {
		return immutableEntry(new String(next.getKey(), UTF_8), new String(next.getValue(), UTF_8));
	}
}