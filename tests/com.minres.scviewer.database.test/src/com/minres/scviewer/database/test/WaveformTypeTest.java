/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.sqlite.SQLiteDbLoader;
import com.minres.scviewer.database.sqlite.TxStream;
import com.minres.scviewer.database.text.TextDbLoader;

public class WaveformTypeTest {
    private void checkWaveform(IWaveform waveform, WaveformType expected) throws Exception {
        assertEquals(expected, waveform.getType());
    }

    @Test
    public void transactionTypesIncludeStreamsAndGenerators() {
        for (WaveformType type : WaveformType.values())
            assertEquals(type == WaveformType.STREAM || type == WaveformType.GENERATOR, type.isTransaction());
    }

    @Test
    public void ftrAndTextClassifyWaveformsBeforePublishingThem() throws Exception {
        for (String input : new String[] {"my_db.ftr", "my_db_c.ftr", "my_db.txlog", "my_db_truncated.txlog"}) {
            IWaveformDbLoader loader = input.endsWith("txlog") ? new TextDbLoader() : new FtrDbLoader();
            List<PropertyChangeEvent> published = new ArrayList<>();
            loader.addPropertyChangeListener(event -> {
                if (IWaveformDbLoader.STREAM_ADDED.equals(event.getPropertyName())
                        || IWaveformDbLoader.GENERATOR_ADDED.equals(event.getPropertyName())) {
                    WaveformType expected = IWaveformDbLoader.STREAM_ADDED.equals(event.getPropertyName())
                            ? WaveformType.STREAM : WaveformType.GENERATOR;
                    assertEquals(expected, ((IWaveform) event.getNewValue()).getType());
                    published.add(event);
                }
            });
            try {
                loader.load(TxTestSupport.input(input));
                assertEquals(8, published.size());
                for (IWaveform waveform : loader.getAllWaves())
                    checkWaveform(waveform, waveform.getParent() instanceof IWaveform
                            ? WaveformType.GENERATOR : WaveformType.STREAM);
            } finally { loader.dispose(); }
        }
    }

    @Test
    public void sqliteClassifiesStreamsAndLazilyLoadedGenerators() throws Exception {
        SQLiteDbLoader loader = new SQLiteDbLoader();
        try {
            loader.load(TxTestSupport.input("my_sqldb.txdb"));
            int streams = 0, generators = 0;
            for (IWaveform stream : loader.getAllWaves()) {
                streams++;
                assertEquals(WaveformType.STREAM, stream.getType());
                for (IWaveform generator : ((TxStream) stream).getGenerators()) {
                    generators++;
                    checkWaveform(generator, WaveformType.GENERATOR);
                }
                checkWaveform(stream, WaveformType.STREAM);
            }
            assertEquals(3, streams);
            assertEquals(5, generators);
        } finally { loader.dispose(); }
    }

    @Test
    public void vcdAndFstStillClassifySignalsCorrectly() throws Exception {
        for (String input : new String[] {"my_db.vcd", "my_db.fst"}) {
            IWaveformDb database = TestWaveformDbFactory.getDatabase();
            try {
                assertTrue(database.load(TxTestSupport.input(input)));
                assertEquals(14, database.getAllWaves().size());
                for (IWaveform signal : database.getAllWaves())
                    checkWaveform(signal, WaveformType.SIGNAL);
            } finally { database.close(); }
        }
    }
}
