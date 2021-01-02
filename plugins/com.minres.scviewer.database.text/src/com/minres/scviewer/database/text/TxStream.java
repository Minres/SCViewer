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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.mapdb.BTreeMap;
import org.mapdb.Serializer;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxGenerator;

class TxStream extends HierNode implements IWaveform, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6721893017334753858L;

	private Long id;
			
	private ArrayList<ITxGenerator> generators = new ArrayList<ITxGenerator>();
	
	private int maxConcurrency;
	
	private BTreeMap<Long, IEvent[]> events;
	
	@SuppressWarnings("unchecked")
	TxStream(TextDbLoader loader, Long id, String name, String kind){
		super(name);
		this.id=id;
		this.maxConcurrency=0;
		//events = new TreeMap<Long, List<ITxEvent>>()
		events = (BTreeMap<Long, IEvent[]>) loader.mapDb.treeMap(name).keySerializer(Serializer.LONG).createOrOpen();
	}

	List<ITxGenerator> getGenerators(){
		return generators;
	}

	@Override
	public int getWidth() {
		if(maxConcurrency==0){
			for(ITxGenerator generator:getGenerators()) {
				for(ITx tx:generator.getTransactions()){
					putEvent(new TxEvent(EventKind.BEGIN, tx));
					putEvent(new TxEvent(EventKind.END, tx));
				}
			}
			ArrayList<Long> rowendtime = new ArrayList<Long>();
			rowendtime.add(0l);
			for(Long time: events.keySet()){
				IEvent[] value=events.get(time);
				Arrays.asList(value).stream().filter(event -> event.getKind()==EventKind.BEGIN).forEach(event -> {
					ITx tx = ((ITxEvent)event).getTransaction();
					int rowIdx = 0;
					for(rowIdx=0; rowIdx<rowendtime.size() && rowendtime.get(rowIdx)>tx.getBeginTime(); rowIdx++);
					if(rowendtime.size()<=rowIdx)
						rowendtime.add(tx.getEndTime());
					else
						rowendtime.set(rowIdx, tx.getEndTime());
					((Tx)tx).setConcurrencyIndex(rowIdx);
					
				});
			}
			maxConcurrency=rowendtime.size();
		}
		return maxConcurrency;
	}

	private void putEvent(ITxEvent event){
		if(!events.containsKey(event.getTime())) 
			events.put(event.getTime(), new ITxEvent[]{event} );
		else {
			IEvent[] entries = events.get(event.getTime());
			IEvent[] newEntries = Arrays.copyOf(entries, entries.length+1);
			newEntries[entries.length]=event;
			events.put(event.getTime(), newEntries);
		}
	}
	
	@Override
	public NavigableMap<Long, IEvent[]> getEvents() {
		return (NavigableMap<Long, IEvent[]>)events;
	}

	@Override
	public IEvent[] getEventsAtTime(Long time) {
		return events.get(time);
	}
	
	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxStream && this.getId()==other.getId());
	}

	@Override
	public IEvent[] getEventsBeforeTime(Long time) {
    	Entry<Long, IEvent[]> e = events.floorEntry(time);
    	if(e==null)
    		return null;
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

}
