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
package com.minres.scviewer.database.tx;

import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;

/** Immutable balanced interval index; each transaction is represented by its start event. */
public final class TxIntervalIndex {
    private static final class Node {
        final ITxEvent event;
        final long begin, end, maxEnd;
        final Node left, right;

        Node(List<ITxEvent> events, int from, int to) {
            com.minres.scviewer.database.ReadCancellation.check();
            int mid = (from + to) >>> 1;
            event = events.get(mid);
            ITx tx = event.getTransaction();
            begin = tx.getBeginTime();
            end = tx.getEndTime();
            left = from < mid ? new Node(events, from, mid) : null;
            right = mid + 1 < to ? new Node(events, mid + 1, to) : null;
            long max = end;
            if (left != null && Long.compareUnsigned(left.maxEnd, max) > 0) max = left.maxEnd;
            if (right != null && Long.compareUnsigned(right.maxEnd, max) > 0) max = right.maxEnd;
            maxEnd = max;
        }

        void overlapping(long from, long to, List<ITxEvent> result) {
            if (Long.compareUnsigned(maxEnd, from) < 0) return;
            if (left != null) left.overlapping(from, to, result);
            if (Long.compareUnsigned(begin, to) > 0) return;
            if (Long.compareUnsigned(end, from) >= 0) result.add(event);
            if (right != null) right.overlapping(from, to, result);
        }
    }

    private final Node root;

    public TxIntervalIndex(IEventList events) {
        List<ITxEvent> starts = new ArrayList<>();
        for (EventEntry entry : events) {
            com.minres.scviewer.database.ReadCancellation.check();
            for (IEvent event : entry.events)
                if (event instanceof ITxEvent && event.getKind() != EventKind.END)
                    starts.add((ITxEvent) event);
        }
        // EventList already orders buckets by timestamp, including same-time starts.
        root = starts.isEmpty() ? null : new Node(starts, 0, starts.size());
    }

    /** Inclusive boundaries also include zero-duration transactions. */
    public List<ITxEvent> overlapping(long from, long to) {
        if (Long.compareUnsigned(from, to) > 0) throw new IllegalArgumentException("Reversed time range");
        List<ITxEvent> result = new ArrayList<>();
        if (root != null) root.overlapping(from, to, result);
        return result;
    }
}
