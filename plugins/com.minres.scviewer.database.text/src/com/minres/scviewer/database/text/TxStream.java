/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxGenerator;

class TxStream extends HierNode implements IWaveform {

	private Long id;
			
	private TextDbLoader loader;
	
	final String kind;
	
	private int maxConcurrency = 0;
	
	private int concurrency = 0;
 
	boolean concurrencyCalculated = false;
 
	void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
		if(concurrency>maxConcurrency)
			maxConcurrency = concurrency;
	}

	int getConcurrency() {
		return this.concurrency;
	}

	TreeMap<Long, IEvent[]> events = new TreeMap<>();
	
	TxStream(TextDbLoader loader, Long id, String name, String kind){
		super(name);
		this.id=id;
		this.loader=loader;
		this.kind=kind;
	}

	List<ITxGenerator> getGenerators(){
		return new ArrayList<>(loader.txGenerators.values());
	}

	@Override
	public int getWidth() {
		return maxConcurrency;
	}

	public void addEvent(ITxEvent evt) {
		if(!events.containsKey(evt.getTime()))
			events.put(evt.getTime(), new IEvent[] {evt});
		else {
			IEvent[] evts = events.get(evt.getTime());
			IEvent[] newEvts = Arrays.copyOf(evts, evts.length+1);
			newEvts[evts.length]=evt;
			events.put(evt.getTime(), newEvts);
		}
	}
	
	@Override
	public NavigableMap<Long, IEvent[]> getEvents() {
		if(!concurrencyCalculated) calculateConcurrency();
		return events;
	}

	@Override
	public IEvent[] getEventsAtTime(Long time) {
		if(!concurrencyCalculated) calculateConcurrency();
		return events.get(time);
	}
	
	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxStream && this.getId().equals(other.getId()));
	}

	@Override
	public IEvent[] getEventsBeforeTime(Long time) {
		if(!concurrencyCalculated)
			calculateConcurrency();
    	Entry<Long, IEvent[]> e = events.floorEntry(time);
    	if(e==null)
    		return new IEvent[] {};
    	else
    		return  events.floorEntry(time).getValue();
	}

	@Override
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}

	@Override
	public Long getId() {
		return id;
	}

	synchronized void calculateConcurrency() {
		if(concurrencyCalculated) return;
		ArrayList<Long> rowendtime = new ArrayList<>();
		events.entrySet().stream().forEach( entry -> {
			IEvent[] values = entry.getValue();
			Arrays.asList(values).stream().filter(e->e.getKind()==EventKind.BEGIN).forEach(evt -> {
				Tx tx = (Tx) ((TxEvent)evt).getTransaction();
				int rowIdx = 0;
				for(; rowIdx<rowendtime.size() && rowendtime.get(rowIdx)>tx.getBeginTime(); rowIdx++);
				if(rowendtime.size()<=rowIdx)
					rowendtime.add(tx.getEndTime());
				else
					rowendtime.set(rowIdx, tx.getEndTime());
				tx.setConcurrencyIndex(rowIdx);
			});
		});
		concurrencyCalculated=true;
	}

}
