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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.sqlite.db.IDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabase;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabaseSelectHandler;
import com.minres.scviewer.database.sqlite.tables.ScvSimProps;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.sqlite.tables.ScvTxEvent;

public class SQLiteDbLoader implements IWaveformDbLoader {

	protected IDatabase database;
	
	private List<RelationType> usedRelationsList = new ArrayList<>();
	
	private ScvSimProps scvSimProps;
		
	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	@Override
	public long getMaxTime() {
		SQLiteDatabaseSelectHandler<ScvTxEvent> handler = new SQLiteDatabaseSelectHandler<>(ScvTxEvent.class,
				database, "time = (SELECT MAX(time) FROM ScvTxEvent)");
		try {
			List<ScvTxEvent> event = handler.selectObjects();
			if(!event.isEmpty())
				return event.get(0).getTime()*scvSimProps.getTime_resolution();
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			e.printStackTrace();
		}
		return 0L;
	}

	@Override
	public Collection<IWaveform> getAllWaves() {
		SQLiteDatabaseSelectHandler<ScvStream> handler = new SQLiteDatabaseSelectHandler<>(ScvStream.class, database);
		List<IWaveform> streams=new ArrayList<>();
		try {
			for(ScvStream scvStream:handler.selectObjects()){
				TxStream stream = new TxStream(database, scvStream);
				stream.setRelationTypeList(usedRelationsList);
				streams.add(stream);
			}
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
		}
		return streams;
	}

//	@Override
//	public boolean canLoad(File inputFile) {
//		if (!inputFile.isDirectory() && inputFile.exists()) {
//			try(InputStream stream = new FileInputStream(inputFile)){
//				byte[] buffer = new byte[x.length];
//				int readCnt = stream.read(buffer, 0, x.length);
//				if (readCnt == x.length) {
//					for (int i = 0; i < x.length; i++)
//						if (buffer[i] != x[i])
//							return false;
//				}
//				return true;
//			} catch (Exception e) {
//				return false;
//			}
//		}
//		return false;
//	}

	@Override
	public void load(File file) throws InputFormatException {
		database=new SQLiteDatabase(file.getAbsolutePath());
		database.setData("TIMERESOLUTION", 1L);
		SQLiteDatabaseSelectHandler<ScvSimProps> handler = new SQLiteDatabaseSelectHandler<>(ScvSimProps.class, database);
		try {
			for(ScvSimProps simProps:handler.selectObjects()){
				scvSimProps=simProps;
				database.setData("TIMERESOLUTION", scvSimProps.getTime_resolution());
			}
			pcs.firePropertyChange(IWaveformDbLoader.LOADING_FINISHED, null, null);
		} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
			throw new InputFormatException(e.toString());
		}
	}

	public void dispose() {
		database=null;
		usedRelationsList=null;
	}

	@Override
	public Collection<RelationType> getAllRelationTypes(){
		return usedRelationsList;
	}

	/**
	 * Adds the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * Removes the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

}
