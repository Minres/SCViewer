package com.minres.scviewer.database.leveldb;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;

import org.iq80.leveldb.Options;
import org.iq80.leveldb.Range;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.iq80.leveldb.impl.DbImpl;
import org.iq80.leveldb.impl.SeekingIterator;

class TxDBWrapper {
	private final Options options;
	private final ReadOptions ro = new ReadOptions();
	private final File databaseDir;
	private DbImpl db;
	private long timeResolution=1L;;

	TxDBWrapper(Options options, File databaseDir) throws IOException {
		this.options = options.verifyChecksums(true).createIfMissing(false).errorIfExists(false).cacheSize(64*1024*1024);
		this.databaseDir = databaseDir;
		this.db = new DbImpl(options, databaseDir);
		ro.snapshot(db.getSnapshot());
	}

	public String get(String key) {
		byte[] slice = db.get(LevelDBLoader.toByteArray(key));
		if (slice == null) {
			return null;
		}
		return new String(slice, UTF_8);
	}

	public String get(String key, Snapshot snapshot) {
		byte[] slice = db.get(LevelDBLoader.toByteArray(key), ro);
		return slice == null? null : new String(slice, UTF_8);
	}

	public void put(String key, String value) {
		db.put(LevelDBLoader.toByteArray(key), LevelDBLoader.toByteArray(value));
	}

	public void delete(String key) {
		db.delete(LevelDBLoader.toByteArray(key));
	}

	public SeekingIterator<String, String> iterator() {
		return new StringDbIterator(db.iterator());
	}

	public Snapshot getSnapshot() {
		return db.getSnapshot();
	}

	public void close() {
		try {
			ro.snapshot().close();
			db.close();
		} catch (IOException e) {} // ignore any error
	}

	public long size(String start, String limit) {
		return db.getApproximateSizes(new Range(LevelDBLoader.toByteArray(start), LevelDBLoader.toByteArray(limit)));
	}

	public long getMaxNextLevelOverlappingBytes() {
		return db.getMaxNextLevelOverlappingBytes();
	}

	public void reopen() throws IOException {
		reopen(options);
	}

	public void reopen(Options options) throws IOException {
		this.close();
		db = new DbImpl(options.verifyChecksums(true).createIfMissing(false).errorIfExists(false), databaseDir);
		ro.snapshot(db.getSnapshot());
	}
	
	public long getTimeResolution() {
		return timeResolution;
	}
	
	public void setTimeResolution(long resolution) {
		this.timeResolution = resolution;
	}
}