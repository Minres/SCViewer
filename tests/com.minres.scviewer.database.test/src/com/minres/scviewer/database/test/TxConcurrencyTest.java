package com.minres.scviewer.database.test;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.sqlite.TxStream;
import com.minres.scviewer.database.sqlite.db.SQLiteDatabase;
import com.minres.scviewer.database.sqlite.tables.ScvStream;
import com.minres.scviewer.database.text.TextDbLoader;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

public class TxConcurrencyTest {
    private void checkRows(IWaveform wave) throws Exception {
        for (EventEntry entry : wave.getEvents()) for (IEvent raw : entry.events) {
            ITxEvent event = (ITxEvent) raw;
            ITx tx = event.getTransaction();
            // The property getters must initialize lazy layouts without a prior getRowCount().
            int streamIndex = tx.getConcurrencyIndxInStream();
            int generatorIndex = tx.getConcurrencyIdxInGen();
            assertTrue(streamIndex >= 0 && streamIndex < tx.getStream().getRowCount());
            assertTrue(generatorIndex >= 0 && generatorIndex < tx.getGenerator().getRowCount());
            assertEquals(wave == tx.getStream() ? streamIndex : generatorIndex, event.getRowIndex());
            if (event instanceof com.minres.scviewer.database.sqlite.TxEvent)
                assertEquals(event.getRowIndex(), ((ITxEvent) event.duplicate()).getRowIndex());
        }
    }

    @Test
    public void ftrAndTextPropertiesMatchBothEventLayouts() throws Exception {
        for (String input : new String[] {"my_db.ftr", "my_db_c.ftr", "my_db.txlog"}) {
            IWaveformDbLoader loader = input.endsWith("txlog") ? new TextDbLoader() : new FtrDbLoader();
            try {
                loader.load(TxTestSupport.input(input));
                for (IWaveform wave : loader.getAllWaves()) checkRows(wave);
            } finally { loader.dispose(); }
        }
    }

    private Map<Long, ITx> transactions(IWaveform stream) {
        Map<Long, ITx> transactions = new HashMap<>();
        for (EventEntry entry : stream.getEvents()) for (IEvent raw : entry.events) {
            ITx tx = ((ITxEvent) raw).getTransaction();
            transactions.put(tx.getId(), tx);
        }
        return transactions;
    }

    private void checkIndependentRows(IWaveform stream) throws Exception {
        Map<Long, ITx> tx = transactions(stream);
        assertEquals(0, tx.get(10L).getConcurrencyIndxInStream());
        assertEquals(0, tx.get(10L).getConcurrencyIdxInGen());
        assertEquals(0, tx.get(11L).getConcurrencyIdxInGen());
        assertEquals(1, tx.get(11L).getConcurrencyIndxInStream());
        assertEquals(2, tx.get(12L).getConcurrencyIndxInStream());
        assertEquals(1, tx.get(12L).getConcurrencyIdxInGen());
        assertEquals(1, tx.get(13L).getConcurrencyIndxInStream());
        assertEquals(0, tx.get(13L).getConcurrencyIdxInGen());
        assertEquals(2, tx.get(11L).getGenerator().getRowCount());
        checkRows(stream);
        for (com.minres.scviewer.database.IHierNode child : stream.getChildNodes()) checkRows((IWaveform) child);
    }

    @Test
    public void textRowsAreIndependentAcrossGeneratorsAndSurviveReload() throws Exception {
        Path file = Files.createTempFile("tx-concurrency-", ".txlog");
        TextDbLoader loader = new TextDbLoader();
        try {
            Files.writeString(file, "scv_tr_stream (ID 1, name \"stream\", kind \"test\")\n"
                    + "scv_tr_generator (ID 4, name \"a\", scv_tr_stream 1,\n)\n"
                    + "scv_tr_generator (ID 5, name \"b\", scv_tr_stream 1,\n)\n"
                    + "tx_begin 10 4 0 ns\ntx_begin 11 5 1 ns\ntx_begin 12 5 2 ns\n"
                    + "tx_end 12 5 3 ns\ntx_end 11 5 4 ns\ntx_begin 13 5 4 ns\n"
                    + "tx_end 13 5 4 ns\ntx_end 10 4 10 ns\n");
            loader.load(file.toFile());
            IWaveform stream = loader.getAllWaves().stream().filter(w -> w.getId() == 1).findFirst().get();
            ITx before = transactions(stream).get(10L);
            checkIndependentRows(stream);
            loader.load(file.toFile());
            stream = loader.getAllWaves().stream().filter(w -> w.getId() == 1).findFirst().get();
            assertNotSame(before, transactions(stream).get(10L));
            checkIndependentRows(stream);
        } finally { loader.dispose();Files.deleteIfExists(file); }
    }

    @Test
    public void sqliteRowsAreIndependentAcrossGenerators() throws Exception {
        Path file = Files.createTempFile("tx-concurrency-", ".txdb");
        try {
            Files.copy(TxTestSupport.input("my_sqldb.txdb").toPath(), file,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            SQLiteDatabase database = new SQLiteDatabase(file.toString());
            database.setData("TIMERESOLUTION", 1L);
            try (Connection connection = database.createConnection();Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM ScvTxRelation");
                statement.executeUpdate("DELETE FROM ScvTxAttribute");
                statement.executeUpdate("DELETE FROM ScvTxEvent");
                statement.executeUpdate("DELETE FROM ScvTx");
                statement.executeUpdate("INSERT INTO ScvTx VALUES (10,4,1,0),(11,5,1,1),(12,5,1,2),(13,5,1,1)");
                statement.executeUpdate("INSERT INTO ScvTxEvent VALUES (10,0,0),(10,2,10),(11,0,1),(11,2,4),"
                        + "(12,0,2),(12,2,3),(13,0,4),(13,2,4)");
            }
            ScvStream row = new ScvStream();row.setId(1);row.setName("stream");row.setKind("test");
            checkIndependentRows(new TxStream(database, row));
        } finally { Files.deleteIfExists(file); }
    }
}
