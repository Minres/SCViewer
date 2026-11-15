/*******************************************************************************
 * Copyright (c) 2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
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
