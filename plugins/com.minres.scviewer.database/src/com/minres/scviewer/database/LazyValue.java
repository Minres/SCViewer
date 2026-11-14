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
