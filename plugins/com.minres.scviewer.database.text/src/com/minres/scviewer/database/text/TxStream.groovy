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
package com.minres.scviewer.database.text

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Map.Entry

import org.mapdb.Serializer

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb
import com.minres.scviewer.database.WaveformType
import com.minres.scviewer.database.tx.ITx
import com.minres.scviewer.database.tx.ITxEvent
import com.minres.scviewer.database.tx.ITxGenerator
import com.minres.scviewer.database.EventKind
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent
import com.minres.scviewer.database.IHierNode

class TxStream extends HierNode implements IWaveform {

	Long id
	
	IWaveformDb database
	
	String fullName
	
	def generators = []
	
	int maxConcurrency
	
	private TreeMap<Long, IEvent[]> events
	
	TxStream(TextDbLoader loader, int id, String name, String kind){
		super(name)
		this.id=id
		this.database=loader.db
		this.fullName=name
		this.maxConcurrency=0
		//events = new TreeMap<Long, List<ITxEvent>>()
		events = loader.mapDb.treeMap(name).keySerializer(Serializer.LONG).createOrOpen();
	}

	List<ITxGenerator> getGenerators(){
		return generators as List<ITxGenerator>
	}

	@Override
	public IWaveformDb getDb() {
		return database
	}

	@Override
	public int getWidth() {
		if(!maxConcurrency){
			generators.each {TxGenerator generator ->
				generator.transactions.each{ Tx tx ->
					putEvent(new TxEvent(EventKind.BEGIN, tx))
					putEvent(new TxEvent(EventKind.END, tx))
				}
			}
			def rowendtime = [0]
			events.keySet().each{long time ->
				def value=events.get(time)
				def starts=value.findAll{IEvent event ->event.kind==EventKind.BEGIN}
				starts.each {ITxEvent event ->
					Tx tx = event.transaction
					def rowIdx = 0
					for(rowIdx=0; rowIdx<rowendtime.size() && rowendtime[rowIdx]>tx.beginTime; rowIdx++);
					if(rowendtime.size<=rowIdx)
						rowendtime<<tx.endTime
					else
						rowendtime[rowIdx]=tx.endTime
					tx.concurrencyIndex=rowIdx
				}
			}
			maxConcurrency=rowendtime.size()
		}
		return maxConcurrency
	}

	private putEvent(ITxEvent event){
		if(!events.containsKey(event.time)) 
			events.put(event.time, [event] as IEvent[])
		else {
			def entries = events[event.time] as List
			entries<<event
			events.put(event.time, entries as IEvent[])
		}
	}
	
	@Override
	public NavigableMap<Long, IEvent[]> getEvents() {
		return events;
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

}
