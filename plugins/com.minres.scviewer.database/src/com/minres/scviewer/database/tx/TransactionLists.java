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
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;

public final class TransactionLists {
    private TransactionLists() {}

    /** Compatibility path for event-backed waveforms; callers can cache the completed result. */
    public static List<ITx> fromEvents(IEventList events, BooleanSupplier cancelled) {
        List<ITx> transactions = new ArrayList<>();
        for (EventEntry entry : events.entrySet()) {
            if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
            if (entry.events == null) continue;
            for (IEvent event : entry.events) {
                if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
                if (event instanceof ITxEvent tx && (event.getKind() == EventKind.BEGIN || event.getKind() == EventKind.SINGLE))
                    transactions.add(tx.getTransaction());
            }
        }
        return sorted(transactions);
    }

    public static List<ITx> sorted(java.util.Collection<ITx> transactions) {
        List<ITx> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparingLong(ITx::getBeginTime).thenComparingLong(ITx::getId));
        return List.copyOf(sorted);
    }
}
