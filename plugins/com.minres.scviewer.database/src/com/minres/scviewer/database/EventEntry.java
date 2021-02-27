package com.minres.scviewer.database;

import java.util.Arrays;

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


	public void append(IEvent value) {
		if(events.length==0)
			events = new IEvent[] {value};
		else {
			int idx = events.length;
			events = Arrays.copyOf(events, idx+1);
			events[idx]=value;
		}
	}
}
