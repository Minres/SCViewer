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
import org.iq80.leveldb.impl.SeekingIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxGenerator;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.database.ITxStream;

public class Tx implements ITx {

	private TxDBWrapper levelDb;
	private TxStream trStream;
	private TxGenerator trGenerator;
	private long id;
	private long start_time=0;
	private long end_time=0;
	private int concurency_index;
	private boolean initialized=false;
	private List<ITxAttribute> attributes;
	private List<ITxRelation> incoming, outgoing;
	
	public Tx(TxDBWrapper levelDb, TxStream trStream, TxGenerator trGenerator, long id) {
		this.levelDb=levelDb;
		this.trStream=trStream;
		this.trGenerator=trGenerator;
		this.id=id;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public ITxStream<ITxEvent> getStream() {
		return trStream;
	}

	@Override
	public ITxGenerator getGenerator() {
		return trGenerator;
	}

	@Override
	public int getConcurrencyIndex() {
		if(!initialized) loadFromDb();
		return concurency_index;
	}

	@Override
	public Long getBeginTime() {
		if(!initialized) loadFromDb();
		return start_time;
	}

	@Override
	public Long getEndTime() {
		loadFromDb();
		return end_time;
	}

	@Override
	public List<ITxAttribute> getAttributes() {
		if(attributes==null) {
			loadFromDb();
		}
		return attributes;
	}

	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		if(incoming==null) {
			incoming = new ArrayList<ITxRelation>();
			SeekingIterator<String, String> it = levelDb.iterator();
			String key = "ri~"+String.format("%016x", id);
			it.seek(key);
			while(it.hasNext()) {
				String val = it.next().getKey();
				if(!val.startsWith(key)) break;;
				String[] token = val.split("~");
				long otherId = Long.parseLong(token[2], 16);
				incoming.add(createRelation(otherId, token[3], false));
			}

		}
		return incoming;
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		if(outgoing==null) {
			outgoing = new ArrayList<ITxRelation>();
			SeekingIterator<String, String> it = levelDb.iterator();
			String key="ro~"+String.format("%016x", id);
			it.seek(key);
			while(it.hasNext()) {
				String val = it.next().getKey();
				if(!val.startsWith(key)) break;
				String[] token = val.split("~");
				long otherId = Long.parseLong(token[2], 16);
				outgoing.add(createRelation(otherId, token[3], true));
			}

		}
		return outgoing;
	}

	@Override
	public int compareTo(ITx o) {
		int res = this.getBeginTime().compareTo(o.getBeginTime());
		if(res!=0)	
			return res;
		else
			return this.getId().compareTo(o.getId());
	}

	@Override
	public String toString() {
		return "tx#"+getId()+"["+getBeginTime()/1000000+"ns - "+getEndTime()/1000000+"ns]";
	}
	
	private void loadFromDb() throws JSONException {
		JSONObject dbVal = new JSONObject(levelDb.get("x~"+ String.format("%016x", id)));
		start_time=dbVal.getLong("START_TIME") * levelDb.getTimeResolution();
		end_time=dbVal.getLong("END_TIME") * levelDb.getTimeResolution();
		concurency_index=dbVal.getInt("conc");
		attributes=new ArrayList<>();
		JSONArray arr = dbVal.getJSONArray("attr");
		arr.forEach(entry -> {
			TxAttribute attr = new TxAttribute(this, (JSONObject) entry);
			attributes.add(attr);
		});
		initialized=true;
	}

	private ITxRelation createRelation(long otherId, String name, boolean outgoing) {
		try {
			JSONObject otherTxVal = new JSONObject(levelDb.get("x~"+ String.format("%016x", otherId)));
			if(otherTxVal.isEmpty()) return null;
			JSONObject otherStreamVal = new JSONObject(levelDb.get("s~"+ String.format("%016x", otherTxVal.getLong("s"))));
			if(otherStreamVal.isEmpty()) return null;
			TxStream tgtStream = (TxStream) trStream.getDb().getStreamByName(otherStreamVal.getString("name"));
			Tx that = (Tx) tgtStream.getTransactions().get(otherId);
			return outgoing?
					new TxRelation(trStream.getRelationType(name), this, that):
						new TxRelation(trStream.getRelationType(name), that, this);
		} catch (SecurityException | IllegalArgumentException | JSONException e) {
			e.printStackTrace();
		}
		return null;		
	}

}
