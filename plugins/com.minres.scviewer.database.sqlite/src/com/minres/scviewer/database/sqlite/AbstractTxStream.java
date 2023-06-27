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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.tx.ITx;

abstract class AbstractTxStream extends HierNode implements IWaveform {

	protected IDatabase database;

	private long streamId;
	
	private Integer maxConcurrency;

	private IEventList events;

	private List<RelationType> usedRelationsList;

	protected AbstractTxStream(IDatabase database, String name, long streamId) {
		super(name);
		this.database=database;
		this.streamId=streamId;
	}

	@Override
	public int getRowCount() {
		if(maxConcurrency==null){
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT MAX(concurrencyLevel) as concurrencyLevel FROM ScvTx where stream=");
			sb.append(streamId);
			try(
					java.sql.Connection connection = database.createConnection();
					java.sql.Statement statement = connection.createStatement();
					java.sql.ResultSet resultSet = statement.executeQuery(sb.toString());
					) {
				while (resultSet.next()) {
					if(maxConcurrency==null) maxConcurrency=0;
					Object value = resultSet.getObject("concurrencyLevel");
					if(value!=null)
						maxConcurrency=(Integer) value;
				}
			} catch (SQLException e) {
				if(maxConcurrency==null) maxConcurrency=0;
			}
			maxConcurrency+=1;
		}
		return maxConcurrency;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public  IEventList getEvents(){
		if(events==null){
			events=new EventList();
			for(Entry<Integer, ITx> entry:getTransactions().entrySet()){
				putEvent(new TxEvent(EventKind.BEGIN, entry.getValue()));
				putEvent(new TxEvent(EventKind.END, entry.getValue()));
			}	
		}
		return events;
	}

	private void putEvent(TxEvent ev){
		events.put(ev.getTime(), ev);
	}

	protected abstract Map<Integer, ITx> getTransactions();

	@Override
	public IEvent[] getEventsAtTime(long time) {
		return getEvents().get(time);
	}

	public void setRelationTypeList(List<RelationType> usedRelationsList){
		this.usedRelationsList=usedRelationsList;
	}

	public RelationType getRelationType(String name) {
		RelationType relType=RelationTypeFactory.create(name);
		if(!usedRelationsList.contains(relType)) usedRelationsList.add(relType);
		return relType;
	}

	@Override
	public IEvent[] getEventsBeforeTime(long time) {
		EventEntry e = events.floorEntry(time);
		if(e==null)
			return new IEvent[]{};
		else
			return  events.floorEntry(time).events;
	}

	@Override
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}

	/**
	 * Calculate concurrency.
	 */
	public void calculateConcurrency() {
	}


}
