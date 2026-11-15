/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.minres.scviewer.database.*;
import com.minres.scviewer.database.text.TextDbLoader;
import com.minres.scviewer.database.tx.ITxEvent;

public class TransactionReaderTest {
    @Test
    public void textAndTruncatedRecordingsHaveConsistentRows() throws Exception {
        for (String input : new String[] {"my_db.txlog", "my_db_truncated.txlog"}) {
            TextDbLoader loader = new TextDbLoader();
            try {
                loader.load(TxTestSupport.input(input));
                assertFalse(loader.getAllWaves().isEmpty());
                for (IWaveform wave : loader.getAllWaves()) {
                    int rows = wave.getRowCount();
                    Map<Long, Integer> begun = new HashMap<>();
                    for (EventEntry entry : wave.getEvents()) for (IEvent raw : entry.events) {
                        ITxEvent event = (ITxEvent) raw;
                        assertTrue(event.getRowIndex() >= 0 && event.getRowIndex() < rows);
                        assertTrue(event.getTransaction().getEndTime() >= event.getTransaction().getBeginTime());
                        if (event.getKind() == EventKind.BEGIN)
                            begun.put(event.getTransaction().getId(), event.getRowIndex());
                        else if (event.getKind() == EventKind.END)
                            assertEquals(begun.remove(event.getTransaction().getId()), Integer.valueOf(event.getRowIndex()));
                    }
                    assertTrue(begun.isEmpty());
                    try { wave.getEvents().put(1, null); fail("Frozen event list"); }
                    catch (UnsupportedOperationException expected) { }
                }
            } finally { loader.dispose(); }
        }
    }
}
