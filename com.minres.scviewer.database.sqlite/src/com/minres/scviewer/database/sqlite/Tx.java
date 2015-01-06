package com.minres.scviewer.database.sqlite;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.EventTime;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxGenerator;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvTx;
import com.minres.scviewer.database.sqlite.tables.ScvTxAttribute;
import com.minres.scviewer.database.sqlite.tables.ScvTxEvent;
import com.minres.scviewer.database.sqlite.tables.ScvTxRelation;

public class Tx implements ITx {

	private TxStream trStream;
	private TxGenerator trGenerator;
	private ScvTx scvTx;
	private List<ITxAttribute> attributes;
	private EventTime begin, end;
	private  List<ITxRelation> incoming, outgoing;
	
	public Tx(TxStream trStream, TxGenerator trGenerator, ScvTx scvTx) {
		this.trStream=trStream;
		this.trGenerator=trGenerator;
		this.scvTx=scvTx;
	}

	@Override
	public Long getId() {
		return (long) scvTx.getId();
	}

	@Override
	public ITxStream getStream() {
		return trStream;
	}

	@Override
	public ITxGenerator getGenerator() {
		return trGenerator;
	}

	@Override
	public EventTime getBeginTime() {
		if(begin==null){
		SQLiteDatabaseSelectHandler<ScvTxEvent> handler = new SQLiteDatabaseSelectHandler<ScvTxEvent>(ScvTxEvent.class,
				trStream.getDb().getDb(), "tx="+scvTx.getId()+" AND type="+ AssociationType.BEGIN.ordinal());
		try {
			for(ScvTxEvent scvEvent:handler.selectObjects()){
				begin= new EventTime(scvEvent.getTime()*trStream.getDb().timeResolution, "fs");
			}
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException e) {
		}
		}
		return begin;
	}

	@Override
	public EventTime getEndTime() {
		if(end==null){
		SQLiteDatabaseSelectHandler<ScvTxEvent> handler = new SQLiteDatabaseSelectHandler<ScvTxEvent>(ScvTxEvent.class,
				trStream.getDb().getDb(), "tx="+scvTx.getId()+" AND type="+ AssociationType.END.ordinal());
		try {
			for(ScvTxEvent scvEvent:handler.selectObjects()){
				end = new EventTime(scvEvent.getTime()*trStream.getDb().timeResolution, "fs");
			}
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException e) {
		}
		}
		return end;
	}

	@Override
	public List<ITxAttribute> getAttributes() {
		if(attributes==null){
			SQLiteDatabaseSelectHandler<ScvTxAttribute> handler = new SQLiteDatabaseSelectHandler<ScvTxAttribute>(
					ScvTxAttribute.class, trStream.getDb().getDb(), "tx="+scvTx.getId());
			try {
				attributes = new ArrayList<ITxAttribute>();
				for(ScvTxAttribute scvAttribute:handler.selectObjects()){
					attributes.add(new TxAttribute(this, scvAttribute));
					
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
			}
		}
		return attributes;
	}

	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		if(incoming==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<ScvTxRelation>(
					ScvTxRelation.class, trStream.getDb().getDb(), "sink="+scvTx.getId());
			try {
				incoming = new ArrayList<ITxRelation>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					incoming.add(createRelation(scvRelation, false));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
			}
		}
		return incoming;
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		if(outgoing==null){
			SQLiteDatabaseSelectHandler<ScvTxRelation> handler = new SQLiteDatabaseSelectHandler<ScvTxRelation>(
					ScvTxRelation.class, trStream.getDb().getDb(), "src="+scvTx.getId());
			try {
				outgoing = new ArrayList<ITxRelation>();
				for(ScvTxRelation scvRelation:handler.selectObjects()){
					outgoing.add(createRelation(scvRelation, true));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
			}
		}
		return outgoing;
	}

	private ITxRelation createRelation(ScvTxRelation rel, boolean outgoing) {
		long otherId = outgoing?rel.getSink():rel.getSrc();
		try {
			List<ScvTx> scvTx=new SQLiteDatabaseSelectHandler<ScvTx>(ScvTx.class, trStream.getDb().getDb(), "id="+otherId).selectObjects();
			assert(scvTx.size()==1);
			ITxStream stream = trStream.getDb().getStreamById(scvTx.get(0).getStream());
			Tx that=(Tx) stream.getTransactionById(otherId);
			if(outgoing)
				return new TxRelation(trStream.getDb().getRelationType(rel.getName()), this, that);
			else
				return new TxRelation(trStream.getDb().getRelationType(rel.getName()), that, this);
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException e) {
		}
		return null;		
	}

}