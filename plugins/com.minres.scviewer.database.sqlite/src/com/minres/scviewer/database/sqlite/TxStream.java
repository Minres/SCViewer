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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.LazyValue;

import com.minres.scviewer.database.DirectionType;
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

	private final LazyValue<Map<Integer, ITx>> transactions = new LazyValue<>();
    private final LazyValue<Map<Integer, long[]>> transactionTimes = new LazyValue<>();

    long[] getTransactionTimes(int id) {
        Map<Integer, long[]> times = transactionTimes.get(() -> {
            String sql = "SELECT e.tx, MIN(CASE WHEN e.type=? THEN e.time END) AS beginTime, "
                    + "MAX(CASE WHEN e.type=? THEN e.time END) AS endTime "
                    + "FROM ScvTxEvent e JOIN ScvTx t ON t.id=e.tx WHERE t.stream=? GROUP BY e.tx";
            Map<Integer, long[]> loaded = new HashMap<>();
            long scale = ((Number) database.getData("TIMERESOLUTION")).longValue();
            try (Connection connection = database.createConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, AssociationType.BEGIN.ordinal());
                statement.setInt(2, AssociationType.END.ordinal());
                statement.setLong(3, getId());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        if (rows.getObject("beginTime") == null || rows.getObject("endTime") == null)
                            throw new SQLException("Missing timestamps for transaction " + rows.getInt("tx"));
                        loaded.put(rows.getInt("tx"), new long[] {
                                Math.multiplyExact(rows.getLong("beginTime"), scale),
                                Math.multiplyExact(rows.getLong("endTime"), scale)});
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot load timestamps for stream " + getFullName(), e);
            }
            return loaded;
        });
        long[] result = times.get(id);
        if (result == null) throw new IllegalStateException("Missing timestamps for transaction " + id);
        return result;
    }

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
	public long getId() {
		return (long) scvStream.getId();
	}

	public synchronized List<IWaveform> getGenerators() {
		if(generators==null){
			SQLiteDatabaseSelectHandler<ScvGenerator> handler = new SQLiteDatabaseSelectHandler<>(
					ScvGenerator.class, database, "stream="+scvStream.getId());
			TreeMap<Integer, TxGenerator> loaded = new TreeMap<>();
			try {
				for(ScvGenerator scvGenerator:handler.selectObjects()){
					loaded.put(scvGenerator.getId(), new TxGenerator(database, this, scvGenerator));
				}
                generators = loaded;
			} catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException | SQLException | IntrospectionException | NoSuchMethodException e) {
				throw new IllegalStateException("Cannot load generators for " + getFullName(), e);
			}
		}
		return new ArrayList<>(generators.values());
	}

	protected synchronized Map<Integer, ITx> getTransactions() {
        return transactions.get(() -> {
            if (generators == null) getGenerators();
            TreeMap<Integer, ITx> loaded = new TreeMap<>();
            SQLiteDatabaseSelectHandler<ScvTx> handler = new SQLiteDatabaseSelectHandler<>(ScvTx.class,
                    database, "stream=" + scvStream.getId());
            try {
                for (ScvTx row : handler.selectObjects())
                    loaded.put(row.getId(), new Tx(database, this, generators.get(row.getGenerator()), row));
            } catch (ReflectiveOperationException | SQLException | IntrospectionException e) {
                throw new IllegalStateException("Cannot load transactions for " + getFullName(), e);
            }
            return loaded;
        });
    }

	@Override
	public IEvent[] getEventsAtTime(long time) {
		return getEvents().get(time);
	}

	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxStream && this.getId() == other.getId());
	}

	@Override
	public String getKind() {
		return scvStream.getKind();
	}

	@Override
	public DirectionType getDirection() {
		return DirectionType.IMPLICIT;
	}
}
