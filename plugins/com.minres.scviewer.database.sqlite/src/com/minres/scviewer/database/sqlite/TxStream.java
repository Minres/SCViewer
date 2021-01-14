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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvGenerator;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.sqlite.tables.ScvTx;
import com.minres.scviewer.database.tx.ITx;

public class TxStream extends AbstractTxStream {

	private String fullName;

	private ScvStream scvStream;

	private TreeMap<Integer, TxGenerator> generators;

	private TreeMap<Integer, ITx> transactions;

	public TxStream(IDatabase database, ScvStream scvStream) {
		super(database, scvStream.getName(), scvStream.getId());
		fullName=scvStream.getName();
		this.scvStream=scvStream;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public Long getId() {
		return (long) scvStream.getId();
	}

	public List<IWaveform> getGenerators() {
		if(generators==null){
			SQLiteDatabaseSelectHandler<ScvGenerator> handler = new SQLiteDatabaseSelectHandler<>(
					ScvGenerator.class, database, "stream="+scvStream.getId());
			generators=new TreeMap<>();
			try {
				for(ScvGenerator scvGenerator:handler.selectObjects()){
					generators.put(scvGenerator.getId(), new TxGenerator(database, this, scvGenerator));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<>(generators.values());
	}

	protected Map<Integer, ITx> getTransactions() {
		if(transactions==null){
			if(generators==null) getGenerators();
			transactions = new TreeMap<>();
			SQLiteDatabaseSelectHandler<ScvTx> handler = new SQLiteDatabaseSelectHandler<>(ScvTx.class, database,
					"stream="+scvStream.getId());
			try {
				for(ScvTx scvTx:handler.selectObjects()){
					transactions.put(scvTx.getId(), new Tx(database, this, generators.get(scvTx.getGenerator()), scvTx));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
				e.printStackTrace();
			}
		}
		return transactions;
	}

	@Override
	public IEvent[] getEventsAtTime(Long time) {
		return getEvents().get(time);
	}

	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxStream && this.getId().equals(other.getId()));
	}

	@Override
	public String getKind() {
		return scvStream.getKind();
	}
}
