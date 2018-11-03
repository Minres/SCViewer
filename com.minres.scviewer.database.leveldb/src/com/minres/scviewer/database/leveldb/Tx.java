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

	private StringDBWrapper levelDb;
	private TxStream trStream;
	private TxGenerator trGenerator;
	private long id;
	private JSONObject dbVal;
	private List<ITxAttribute> attributes;
	private List<ITxRelation> incoming, outgoing;
	
	public Tx(StringDBWrapper levelDb, TxStream trStream, TxGenerator trGenerator, long id) {
		this.levelDb=levelDb;
		this.trStream=trStream;
		this.trGenerator=trGenerator;
		this.id=id;
		this.dbVal=null;
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
		checkDb();
		return dbVal.getInt("conc");
	}

	private void checkDb() throws JSONException {
		if(dbVal==null) {
			dbVal = new JSONObject(levelDb.get("x~"+ String.format("%016x", id)));
		}
	}

	@Override
	public Long getBeginTime() {
		checkDb();
		return dbVal.getLong("START_TIME");
	}

	@Override
	public Long getEndTime() {
		checkDb();
		return dbVal.getLong("END_TIME");
	}

	@Override
	public List<ITxAttribute> getAttributes() {
		if(attributes==null) {
			checkDb();
			attributes=new ArrayList<>();
			JSONArray arr = dbVal.getJSONArray("attr");
			arr.forEach(entry -> {
				TxAttribute attr = new TxAttribute(this, (JSONObject) entry);
				attributes.add(attr);
			});
		}
		return attributes;
	}

	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		return incoming;
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
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
}
