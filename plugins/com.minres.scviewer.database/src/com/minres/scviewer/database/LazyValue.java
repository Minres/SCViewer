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
import java.util.function.Supplier;

/** Single-flight lazy initialization. Waiters can cancel without cancelling the owner. */
public final class LazyValue<T> {
    private enum State { UNLOADED, LOADING, LOADED, FAILED }
    private State state = State.UNLOADED;
    private Thread owner;
    private T value;
    private Throwable failure;

    public T get(Supplier<T> supplier) {
        synchronized (this) {
            while (state == State.LOADING) {
                if (owner == Thread.currentThread()) throw new IllegalStateException("Recursive lazy loading");
                ReadCancellation.check();
                try { wait(50); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException(); }
            }
            ReadCancellation.check();
            if (state == State.LOADED) return value;
            if (state == State.FAILED) {
                if (failure instanceof Error error) throw error;
                throw (RuntimeException) failure;
            }
            owner = Thread.currentThread();
            state = State.LOADING;
        }
        try {
            T loaded = supplier.get();
            synchronized (this) { value = loaded; state = State.LOADED; owner = null; notifyAll(); }
            return loaded;
        } catch (RuntimeException | Error e) {
            synchronized (this) {
                if (e instanceof CancellationException) state = State.UNLOADED;
                else { failure = e; state = State.FAILED; }
                owner = null;
                notifyAll();
            }
            throw e;
        }
    }
}
