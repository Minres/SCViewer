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
import java.util.Map;
import java.util.TreeMap;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvGenerator;
import com.minres.scviewer.database.sqlite.tables.ScvTx;
import com.minres.scviewer.database.tx.ITx;

public class TxGenerator extends AbstractTxStream {

	private TxStream  stream;
	
	private ScvGenerator scvGenerator;

	private TreeMap<Integer, ITx> transactions;

	public TxGenerator(IDatabase database, TxStream stream, ScvGenerator scvGenerator) {
		super(database, scvGenerator.getName(), stream.getId());
		this.stream=stream;
		this.scvGenerator=scvGenerator;
		stream.addChild(this);
	}

	@Override
	public Long getId() {
		return (long) scvGenerator.getId();
	}

	@Override
	public String getName() {
		return scvGenerator.getName();
	}

	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxGenerator && this.getId().equals(other.getId()));
	}

	@Override
	public String getKind() {
		return stream.getKind();
	}

	@Override
	protected Map<Integer, ITx> getTransactions() {
		if(transactions==null){
			transactions = new TreeMap<>();
			SQLiteDatabaseSelectHandler<ScvTx> handler = new SQLiteDatabaseSelectHandler<>(ScvTx.class, database,
					"stream="+stream.getId()+" and generator="+scvGenerator.getId());
			try {
				for(ScvTx scvTx:handler.selectObjects()){
					transactions.put(scvTx.getId(), new Tx(database, (TxStream) stream, this, scvTx));
				}
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException e) {
				e.printStackTrace();
			}
		}
		return transactions;
	}

}
