/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.ObjIntConsumer;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;

/** Assigns the lowest available row in O(N log C), where C is peak concurrency. */
public final class TxRowAllocator {
    private record Active(long end, int row) { }
    private TxRowAllocator() { }

    public static int assign(IEventList events, ObjIntConsumer<ITxEvent> setRow) {
        PriorityQueue<Active> active = new PriorityQueue<>((a, b) -> {
            int cmp = Long.compareUnsigned(a.end(), b.end());
            return cmp != 0 ? cmp : Integer.compare(a.row(), b.row());
        });
        PriorityQueue<Integer> free = new PriorityQueue<>();
        Map<Long, Integer> rowByTx = new HashMap<>();
        Map<ITxEvent, Integer> assignments = new IdentityHashMap<>();
        int count = 0;
        for (EventEntry entry : events) {
            for (IEvent raw : entry.events) {
                ITxEvent event = (ITxEvent) raw;
                ITx tx = event.getTransaction();
                if (tx == null) throw new IllegalStateException("Missing transaction for " + event);
                if (event.getKind() == EventKind.END) {
                    Integer row = rowByTx.remove(tx.getId());
                    if (row == null) throw new IllegalStateException("End without begin for transaction " + tx.getId());
                    assignments.put(event, row);
                    continue;
                }
                while (!active.isEmpty() && Long.compareUnsigned(active.peek().end(), tx.getBeginTime()) <= 0)
                    free.add(active.remove().row());
                int row = free.isEmpty() ? count++ : free.remove();
                active.add(new Active(tx.getEndTime(), row));
                assignments.put(event, row);
                if (event.getKind() == EventKind.BEGIN) rowByTx.put(tx.getId(), row);
            }
        }
        // Validate the whole layout before changing any event indexes.
        assignments.forEach((event, row) -> setRow.accept(event, row));
        return Math.max(1, count);
    }
}
