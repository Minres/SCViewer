/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.sqlite;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.sqlite.tables.ScvTx;
import com.minres.scviewer.database.sqlite.tables.ScvTxAttribute;
import com.minres.scviewer.database.sqlite.tables.ScvTxEvent;
import com.minres.scviewer.database.sqlite.tables.ScvTxRelation;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxRelation;

public class Tx implements ITx {

	private IDatabase database;
	private TxStream trStream;
	private TxGenerator trGenerator;
	private ScvTx scvTx;
	private List<ITxAttribute> attributes;
	private long begin=-1;
	private long end=-1;
	private List<ITxRelation> incoming;
	private List<ITxRelation> outgoing;
	
	public Tx(IDatabase database, TxStream trStream, TxGenerator trGenerator, ScvTx scvTx) {
		this.database=database;
		this.trStream=trStream;
		this.trGenerator=trGenerator;
		this.scvTx=scvTx;
	}

	@Override
	public long getId() {
		return (long) scvTx.getId();
	}

	@Override
	public IWaveform getStream() {
		return trStream;
	}

	@Override
	public IWaveform getGenerator() {
		return trGenerator;
	}

	int getConcurrencyIndex() {
		return scvTx.getConcurrencyLevel();
	}

	@Override
	public long getBeginTime() {
		if(begin<0){
		SQLiteDatabaseSelectHandler<ScvTxEvent> handler = new SQLiteDatabaseSelectHandler<>(ScvTxEvent.class,
				database, "tx="+scvTx.getId()+" AND type="+ AssociationType.BEGIN.ordinal());
		try {
			for(ScvTxEvent scvEvent:handler.selectObjects()){
				begin= scvEvent.getTime()*(Long)database.getData("TIMERESOLUTION");
			}
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
		}
		}
		return begin;
	}

	@Override
	public long getEndTime() {
		if(end<0){
		SQLiteDatabaseSelectHandler<ScvTxEvent> handler = new SQLiteDatabaseSelectHandler<>(ScvTxEvent.class,
				database, "tx="+scvTx.getId()+" AND type="+ AssociationType.END.ordinal());
		try {
			for(ScvTxEvent scvEvent:handler.selectObjects()){
				end = scvEvent.getTime()*(Long)database.getData("TIMERESOLUTION");
			}
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
		}
		}
		return end;
	}

	@Override
	public List<ITxAttribute> getAttributes() {
		if(attributes==null){
			SQLiteDatabaseSelectHandler<ScvTxAttribute> handler = new SQLiteDatabaseSelectHandler<>(
					ScvTxAttribute.class, database, "tx="+scvTx.getId());
			try {
				attributes = new ArrayList<>();
				for(ScvTxAttribute scvAttribute:handler.selectObjects()){
					attributes.add(new TxAttribute(this, scvAttribute));
					
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			}
		}
		return attributes;
	}

	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		if(incoming==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<>(
					ScvTxRelation.class, database, "sink="+scvTx.getId());
			try {
				incoming = new ArrayList<>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					incoming.add(createRelation(scvRelation, false));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			}
		}
		return incoming;
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		if(outgoing==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<>(
					ScvTxRelation.class, database, "src="+scvTx.getId());
			try {
				outgoing = new ArrayList<>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					outgoing.add(createRelation(scvRelation, true));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			}
		}
		return outgoing;
	}

	private ITxRelation createRelation(ScvTxRelation rel, boolean outgoing) {
		int otherId = outgoing?rel.getSink():rel.getSrc();
		SQLiteDatabaseSelectHandler<ScvTx> handler = new SQLiteDatabaseSelectHandler<>(ScvTx.class, database,
				"id="+otherId);
		try {
			List<ScvTx> res = handler.selectObjects();
			if(res.size()!=1) return null;
			List<ScvStream> streams = new SQLiteDatabaseSelectHandler<ScvStream>(ScvStream.class, database,
						"id="+res.get(0).getStream()).selectObjects();
			if(streams.size()!=1) return null;
			TxStream tgtStream = (TxStream) database.getWaveformDb().getStreamByName(streams.get(0).getName());
			Tx that = (Tx) tgtStream.getTransactions().get(otherId);
			if(outgoing)
				return new TxRelation(trStream.getRelationType(rel.getName()), this, that);
			else
				return new TxRelation(trStream.getRelationType(rel.getName()), that, this);
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			e.printStackTrace();
		}

		return null;		
	}

	@Override
	public int compareTo(ITx o) {
		int res = Long.compare(this.getBeginTime(), o.getBeginTime());
		if(res!=0)	
			return res;
		else
			return Long.compare(this.getId(), o.getId());
	}

	@Override
	public String toString() {
		return "tx#"+getId()+"["+getBeginTime()/1000000+"ns - "+getEndTime()/1000000+"ns]";
	}
}
