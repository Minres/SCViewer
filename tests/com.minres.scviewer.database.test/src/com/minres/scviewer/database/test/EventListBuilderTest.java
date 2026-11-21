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
import org.junit.Test;
import com.minres.scviewer.database.*;

public class EventListBuilderTest {
    @Test
    public void sortsAndPreservesSameTimeInsertionOrder() {
        EventList.Builder builder = new EventList.Builder();
        TxTestSupport.Tx tx = new TxTestSupport.Tx(1, 0, 10);
        IEvent begin = new TxTestSupport.Event(tx, EventKind.BEGIN);
        IEvent end = new TxTestSupport.Event(tx, EventKind.END);
        builder.add(10, begin);builder.add(2, end);builder.add(10, end);
        builder.add(Long.MIN_VALUE, begin);builder.add(Long.MAX_VALUE, end);
        EventList list = builder.build();
        assertEquals(4, list.size());
        assertEquals(2, list.firstKey());
        assertArrayEquals(new IEvent[] {begin, end}, list.get(10));
        assertEquals(Long.MIN_VALUE, list.lastKey());
        try { list.put(5, begin); fail(); } catch (UnsupportedOperationException expected) { }
        try { builder.add(5, begin); fail(); } catch (IllegalStateException expected) { }
    }

    @Test
    public void handlesManyEventsAtOneTimestampAndEmptyInput() {
        EventList.Builder builder = new EventList.Builder();
        for (int i = 0; i < 10000; ++i) builder.add(5, null);
        assertEquals(10000, builder.build().get(5).length);
        assertNull(new EventList.Builder().build().firstEntry());
    }
}
