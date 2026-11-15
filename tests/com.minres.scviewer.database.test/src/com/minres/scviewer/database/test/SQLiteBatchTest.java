package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Test;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.TxStream;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabase;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

public class SQLiteBatchTest {
    private static class Database extends SQLiteDatabase {
        int connections;
        Database() { super(TxTestSupport.input("my_sqldb.txdb").getAbsolutePath()); setData("TIMERESOLUTION", 1000L); }
        @Override public Connection createConnection() throws SQLException { connections++;return super.createConnection(); }
    }

    @Test
    public void concurrentAttributeReadersShareOneCompletedLoad() throws Exception {
        Database database = new Database();
        ScvStream row = new ScvStream(); row.setId(1); row.setName("test"); row.setKind("test");
        TxStream stream = new TxStream(database, row);
        ITx transaction = stream.getTransactionList().get(0);
        int before = database.connections;
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(8);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.List<java.util.concurrent.Future<java.util.List<com.minres.scviewer.database.tx.ITxAttribute>>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) results.add(workers.submit(() -> { start.await(); return transaction.getAttributes(); }));
            start.countDown();
            java.util.List<com.minres.scviewer.database.tx.ITxAttribute> attributes = results.get(0).get(5, java.util.concurrent.TimeUnit.SECONDS);
            for (java.util.concurrent.Future<?> result : results) assertSame(attributes, result.get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(before + 1, database.connections);
        } finally { workers.shutdownNow(); }
    }

    @Test
    public void loadsAllTimesInOneQueryAndSharesGeneratorTransactions() {
        Database database = new Database();
        ScvStream row = new ScvStream();row.setId(1);row.setName("test");row.setKind("test");
        TxStream stream = new TxStream(database, row);
        assertNotNull(stream.getEventsBeforeTime(0));
        assertEquals(26, stream.getEvents().size());
        // One query each for generators, transaction records, and all timestamps.
        assertEquals(3, database.connections);
        int count = database.connections;
        ITx first = ((ITxEvent)stream.getEventsAtTime(0)[0]).getTransaction();
        assertEquals(0, first.getBeginTime());
        assertEquals(420000000, first.getEndTime());
        for (EventEntry entry : stream.getEvents()) for (IEvent event : entry.events) {
            ITx tx = ((ITxEvent)event).getTransaction();
            assertTrue(tx.getEndTime() >= tx.getBeginTime());
        }
        for (IWaveform generator : stream.getGenerators())
            for (EventEntry entry : generator.getEvents()) for (IEvent event : entry.events)
                if (((ITxEvent)event).getTransaction().getId() == first.getId())
                    assertSame(first, ((ITxEvent)event).getTransaction());
        assertEquals(count, database.connections);
    }
}
