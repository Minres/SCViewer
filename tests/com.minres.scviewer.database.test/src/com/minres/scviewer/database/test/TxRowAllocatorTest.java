package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.tx.TxRowAllocator;

public class TxRowAllocatorTest {
    @Test
    public void randomizedAllocationMatchesLowestFreeRow() {
        Random random = new Random(42);
        EventList events = new EventList();
        for (int i = 0; i < 1000; ++i) {
            int begin = random.nextInt(1000), end = begin + random.nextInt(100);
            TxTestSupport.Tx tx = new TxTestSupport.Tx(i, begin, end);
            events.put(begin, new TxTestSupport.Event(tx, begin == end ? EventKind.SINGLE : EventKind.BEGIN));
            if (begin != end) events.put(end, new TxTestSupport.Event(tx, EventKind.END));
        }
        int count = TxRowAllocator.assign(events, (e, row) -> ((TxTestSupport.Event)e).row = row);
        List<Long> ends = new ArrayList<>();
        java.util.Map<Long, Integer> expected = new java.util.HashMap<>();
        for (EventEntry entry : events) for (IEvent raw : entry.events) {
            TxTestSupport.Event event = (TxTestSupport.Event) raw;
            if (event.kind == EventKind.END) {
                assertEquals((int) expected.remove(event.tx.id), event.row);
            } else {
                int row = 0;
                while (row < ends.size() && ends.get(row) > event.tx.begin) row++;
                if (row == ends.size()) ends.add(event.tx.end);
                else ends.set(row, event.tx.end);
                assertEquals(row, event.row);
                if (event.kind == EventKind.BEGIN) expected.put(event.tx.id, row);
            }
        }
        assertEquals(ends.size(), count);
    }

    @Test
    public void invalidLayoutDoesNotPublishPartialIndexes() {
        TxTestSupport.Tx tx = new TxTestSupport.Tx(1, 0, 10);
        TxTestSupport.Event end = new TxTestSupport.Event(tx, EventKind.END);
        EventList events = new EventList();events.put(10, end);
        try { TxRowAllocator.assign(events, (e, row) -> ((TxTestSupport.Event)e).row = row); fail(); }
        catch (IllegalStateException expected) { assertEquals(-1, end.row); }
        assertEquals(1, TxRowAllocator.assign(new EventList(), (e, row) -> fail()));
    }
}
