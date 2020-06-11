/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.leveldb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iq80.leveldb.impl.SeekingIterator;
import org.json.JSONObject;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Vector;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxGenerator;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.RelationType;

public class TxStream extends HierNode implements ITxStream<ITxEvent> {

	private TxDBWrapper levelDb;

	private String fullName;
	
	private String kind;

	private IWaveformDb db;
		
	private long id;
	
	private TreeMap<Long, TxGenerator> generators;
	
	private TreeMap<Long, ITx> transactions;
	
	private Integer maxConcurrency;
	
	private TreeMap<Long, List<ITxEvent>> events;

	private List<RelationType> usedRelationsList;
	
	public TxStream(TxDBWrapper database, IWaveformDb waveformDb, JSONObject object) {
		super(object.get("name").toString());
		this.levelDb=database;
		this.db=waveformDb;
		this.fullName=object.getString("name");
		this.kind=object.getString("kind");
		this.id = object.getLong("id");
	}

	@Override
	public IWaveformDb getDb() {
		return db;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getKind() {
		return kind;
	}

	@Override
	public List<ITxGenerator> getGenerators() {
		if(generators==null){
			generators=new TreeMap<Long, TxGenerator>();
			SeekingIterator<String, String> it = levelDb.iterator();
			String key="sg~"+String.format("%016x", id);
			it.seek(key);
			while(it.hasNext()) {
				Entry<String, String> val = it.next();
				if(!val.getKey().startsWith(key)) break;
				JSONObject jVal = new JSONObject(val.getValue());
				generators.put(jVal.getLong("id"), new TxGenerator(this, jVal));
			}
		}
		return new ArrayList<ITxGenerator>(generators.values());
	}

	@Override
	public int getMaxConcurrency() {
		if(maxConcurrency==null){
				getTransactions();
		}
		return maxConcurrency;
	}

	@Override
	public  NavigableMap<Long, List<ITxEvent>> getEvents(){
		if(events==null){
			events=new TreeMap<Long, List<ITxEvent>>();
			for(Entry<Long, ITx> entry:getTransactions().entrySet()){
				ITx tx = entry.getValue();
				putEvent(new TxEvent(TxEvent.Type.BEGIN, tx));
				putEvent(new TxEvent(TxEvent.Type.END, tx));
			}	
		}
		return events;
	}

	private void putEvent(TxEvent ev){
		Long time = ev.getTime();
		if(!events.containsKey(time)){
			Vector<ITxEvent> vector=new Vector<ITxEvent>();
			vector.add(ev);
			events.put(time,  vector);
		} else {
			events.get(time).add(ev);
		}
	}

	protected Map<Long, ITx> getTransactions() {
		if(transactions==null){
			if(generators==null) getGenerators();
			transactions = new TreeMap<Long, ITx>();
			maxConcurrency=0;
			SeekingIterator<String, String> it = levelDb.iterator();
			String key = "sgx~"+String.format("%016x", id);
			it.seek(key);
			while(it.hasNext()) {
				Entry<String, String> val = it.next();
				if(!val.getKey().startsWith(key)) break;
				String[] token = val.getKey().split("~");
				long gid = Long.parseLong(token[2], 16); // gen id
				long id = Long.parseLong(token[3], 16); // tx id
				ITx tx = new Tx(levelDb, this, generators.get(gid), id);
				transactions.put(id, tx);
				maxConcurrency= Math.max(maxConcurrency, tx.getConcurrencyIndex());
			}
			maxConcurrency++;
		}
		return transactions;
	}

	@Override
	public Collection<ITxEvent> getWaveformEventsAtTime(Long time) {
		return getEvents().get(time);
	}

	public void setRelationTypeList(List<RelationType> usedRelationsList){
		this.usedRelationsList=usedRelationsList;
	}
	
	public RelationType getRelationType(String name) {
		RelationType relType=RelationType.create(name);
		if(!usedRelationsList.contains(relType)) usedRelationsList.add(relType);
		return relType;
	}

	@Override
	public Boolean equals(IWaveform other) {
		return(other instanceof TxStream && this.getId()==other.getId());
	}

}
