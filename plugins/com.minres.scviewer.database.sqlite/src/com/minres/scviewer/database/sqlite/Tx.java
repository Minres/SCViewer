/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.sqlite.tables.ScvTx;
import com.minres.scviewer.database.sqlite.tables.ScvTxAttribute;
import com.minres.scviewer.database.sqlite.tables.ScvTxRelation;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxRelation;

public class Tx implements ITx {

	private IDatabase database;
	private TxStream trStream;
	private TxGenerator trGenerator;
	private ScvTx scvTx;
    private volatile int concurrencyIdxInGen = -1;
	private List<ITxAttribute> attributes;
	private final long begin;
	private final long end;
	private List<ITxRelation> incoming;
	private List<ITxRelation> outgoing;
	
	public Tx(IDatabase database, TxStream trStream, TxGenerator trGenerator, ScvTx scvTx) {
		this.database=database;
		this.trStream=trStream;
		this.trGenerator=trGenerator;
		this.scvTx=scvTx;
        long[] times = trStream.getTransactionTimes(scvTx.getId());
        begin = times[0];
        end = times[1];
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

    @Override
    public int getConcurrencyIndxInStream() {
        return scvTx.getConcurrencyLevel();
    }

    @Override
    public int getConcurrencyIdxInGen() {
        if (concurrencyIdxInGen < 0) trGenerator.getRowCount();
        return concurrencyIdxInGen;
    }

    void setConcurrencyIdxInGen(int index) {
        concurrencyIdxInGen = index;
    }

	@Override
    public long getBeginTime() { return begin; }

    @Override
    public long getEndTime() { return end; }

	@Override
	public synchronized List<ITxAttribute> getAttributes() {
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
	public synchronized Collection<ITxRelation> getIncomingRelations() {
		if(incoming==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<>(
					ScvTxRelation.class, database, "sink="+scvTx.getId());
			try {
				incoming = new ArrayList<>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					ITxRelation relation = createRelation(scvRelation, false);
					if (relation != null) incoming.add(relation);
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			}
		}
		return List.copyOf(incoming);
	}

	@Override
	public synchronized Collection<ITxRelation> getOutgoingRelations() {
		if(outgoing==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<>(
					ScvTxRelation.class, database, "src="+scvTx.getId());
			try {
				outgoing = new ArrayList<>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					ITxRelation relation = createRelation(scvRelation, true);
					if (relation != null) outgoing.add(relation);
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			}
		}
		return List.copyOf(outgoing);
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
