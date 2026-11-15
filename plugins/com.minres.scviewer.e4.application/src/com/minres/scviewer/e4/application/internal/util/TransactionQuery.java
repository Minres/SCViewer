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
package com.minres.scviewer.e4.application.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.eclipse.core.runtime.OperationCanceledException;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITransactionProvider;
import com.minres.scviewer.database.tx.TransactionLists;

/** Cached transaction headers and property discovery for a selected track. */
public final class TransactionQuery {
    public record Attribute(String name, DataType type) {}
    private final IWaveform waveform;
    private volatile List<ITx> transactions;
    private List<Attribute> attributes;

    public TransactionQuery(IWaveform waveform) { this.waveform = waveform; }

    public List<ITx> transactions(BooleanSupplier cancelled) {
        check(cancelled);
        List<ITx> result = transactions;
        if (result == null) {
            synchronized (this) {
                result = transactions;
                if (result == null) {
                    try {
                        result = waveform instanceof ITransactionProvider indexed ? indexed.getTransactionList()
                                : TransactionLists.fromEvents(waveform.getEvents(), cancelled);
                    } catch (java.util.concurrent.CancellationException e) { throw new OperationCanceledException(); }
                    check(cancelled);
                    transactions = result;
                }
            }
        }
        check(cancelled);
        return result;
    }

    public List<ITx> filter(Predicate<ITx> predicate, BooleanSupplier cancelled) {
        List<ITx> result = new ArrayList<>();
        for (ITx transaction : transactions(cancelled)) {
            check(cancelled);
            if (predicate.test(transaction)) result.add(transaction);
        }
        return List.copyOf(result);
    }

    public synchronized List<Attribute> discoverAttributes(BooleanSupplier cancelled) {
        check(cancelled);
        if (attributes != null) return attributes;
        Map<String, DataType> types = new TreeMap<>();
        for (ITx transaction : transactions(cancelled)) {
            check(cancelled);
            transaction.getAttributes().forEach(attribute -> types.putIfAbsent(attribute.getName(), attribute.getDataType()));
        }
        check(cancelled);
        attributes = types.entrySet().stream().map(e -> new Attribute(e.getKey(), e.getValue())).toList();
        return attributes;
    }

    private static void check(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new OperationCanceledException();
    }
}
