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
	
	private int maxConcurrency = 0;
	
	private int concurrency = 0;

	void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
		if(concurrency>maxConcurrency)
			maxConcurrency = concurrency;
	}

	int getConcurrency() {
		return this.concurrency;
	}

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
		return maxConcurrency;
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
