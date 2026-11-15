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
package com.minres.scviewer.database;

import java.util.function.Supplier;

/** Publishes one complete value, including empty values, and preserves loading failures. */
public final class LazyValue<T> {
    private enum State { UNLOADED, LOADING, LOADED, FAILED }
    private State state = State.UNLOADED;
    private T value;
    private Throwable failure;

    public synchronized T get(Supplier<T> supplier) {
        if (state == State.LOADED) return value;
        if (state == State.FAILED) {
            if (failure instanceof Error) throw (Error) failure;
            throw (RuntimeException) failure;
        }
        if (state == State.LOADING) throw new IllegalStateException("Recursive lazy loading");
        state = State.LOADING;
        try {
            T loaded = supplier.get();
            value = loaded;
            state = State.LOADED;
            return loaded;
        } catch (RuntimeException | Error e) {
            failure = e;
            state = State.FAILED;
            throw e;
        }
    }
}
