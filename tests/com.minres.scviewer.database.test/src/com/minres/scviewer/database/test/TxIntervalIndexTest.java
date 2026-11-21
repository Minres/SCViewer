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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.tx.TxIntervalIndex;

public class TxIntervalIndexTest {
    @Test
    public void spanningViewportAndBoundaries() {
        EventList events = new EventList();
        TxTestSupport.Tx longTx = new TxTestSupport.Tx(1, 0, 1000);
        TxTestSupport.Event begin = new TxTestSupport.Event(longTx, EventKind.BEGIN);
        events.put(0, begin);
        events.put(1000, new TxTestSupport.Event(longTx, EventKind.END));
        TxIntervalIndex index = new TxIntervalIndex(events);
        assertEquals(java.util.Arrays.asList(begin), index.overlapping(400, 500));
        assertEquals(1, index.overlapping(1000, 1000).size());
        assertTrue(index.overlapping(1001, 1100).isEmpty());
        assertTrue(new TxIntervalIndex(new EventList()).overlapping(0, 1).isEmpty());
    }

    @Test
    public void randomizedQueriesMatchFullScan() {
        Random random = new Random(42);
        List<TxTestSupport.Tx> transactions = new ArrayList<>();
        EventList events = new EventList();
        for (int i = 0; i < 500; ++i) {
            int begin = random.nextInt(1000), end = begin + random.nextInt(200);
            TxTestSupport.Tx tx = new TxTestSupport.Tx(i, begin, end);
            transactions.add(tx);
            events.put(begin, new TxTestSupport.Event(tx, begin == end ? EventKind.SINGLE : EventKind.BEGIN));
            if (begin != end) events.put(end, new TxTestSupport.Event(tx, EventKind.END));
        }
        TxIntervalIndex index = new TxIntervalIndex(events);
        for (int i = 0; i < 100; ++i) {
            int from = random.nextInt(1200), to = from + random.nextInt(200);
            List<Long> expected = transactions.stream().filter(t -> t.begin <= to && t.end >= from)
                    .map(t -> t.id).sorted().collect(Collectors.toList());
            assertEquals(expected, index.overlapping(from, to).stream()
                    .map(e -> e.getTransaction().getId()).sorted().collect(Collectors.toList()));
        }
    }
}
