/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Cancellation context for nested lazy reads without depending on Eclipse jobs. */
public final class ReadCancellation {
    private static final ThreadLocal<BooleanSupplier> CURRENT = new ThreadLocal<>();
    private ReadCancellation() { }
    public static void check() {
        BooleanSupplier cancelled = CURRENT.get();
        if (Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.getAsBoolean()))
            throw new CancellationException();
    }
    public static <T> T with(BooleanSupplier cancelled, Supplier<T> query) {
        BooleanSupplier previous = CURRENT.get();
        CURRENT.set(() -> cancelled.getAsBoolean() || (previous != null && previous.getAsBoolean()));
        try { check(); return query.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
