package com.minres.scviewer.database;

public class EventEntry implements Comparable<EventEntry>{
	public long timestamp; // unsigned
	public IEvent[] events = null;
	
	
	public EventEntry(long timestamp) {
		this.timestamp = timestamp;
	}


	public EventEntry(long timestamp, IEvent[] events) {
		this.timestamp = timestamp;
		this.events = events;
	}


	@Override
	public int compareTo(EventEntry o) {
		return Long.compareUnsigned(timestamp, o.timestamp);
	}
	
	@Override
	public String toString() {
		return String.format("e.%d@%d", events.length,timestamp);
	}
}
