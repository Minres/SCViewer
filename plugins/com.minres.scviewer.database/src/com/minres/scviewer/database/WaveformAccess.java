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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/** Coordinates background consumers with database closure. Owners are weakly keyed:
 * stale waveform references retain a closed scope and cannot reopen a recording.
 */
public final class WaveformAccess {
    private static final Map<IWaveform, Scope> OWNERS = new WeakHashMap<>();
    private static final AtomicLong IDS = new AtomicLong();
    private WaveformAccess() { }

    public static final class Scope {
        private final long id = IDS.incrementAndGet();
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        private volatile boolean closing;
        public void register(Collection<IWaveform> waveforms) {
            synchronized (OWNERS) { for (IWaveform waveform : waveforms) OWNERS.put(waveform, this); }
        }
        /** Must run on a worker: waits for existing leases, rejecting new ones. */
        public void close(Runnable cleanup) {
            closing = true;
            lock.writeLock().lock();
            try { cleanup.run(); }
            finally { lock.writeLock().unlock(); }
        }
        private void acquire() {
            try {
                while (!closing) {
                    ReadCancellation.check();
                    if (lock.readLock().tryLock(50, TimeUnit.MILLISECONDS)) {
                        if (!closing) return;
                        lock.readLock().unlock();
                    }
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            throw new CancellationException("Database is closing");
        }
    }

    public static <T> T read(IWaveform waveform, Supplier<T> query) {
        return read(waveform == null ? List.of() : List.of(waveform), query);
    }
    public static <T> T read(Collection<IWaveform> waveforms, Supplier<T> query) {
        Map<Scope, Boolean> scopes = new IdentityHashMap<>();
        synchronized (OWNERS) {
            for (IWaveform waveform : waveforms) collect(waveform, scopes);
        }
        List<Scope> ordered = new ArrayList<>(scopes.keySet());
        ordered.sort(Comparator.comparingLong(scope -> scope.id));
        int acquired = 0;
        try {
            for (Scope scope : ordered) { scope.acquire(); acquired++; }
            ReadCancellation.check();
            return query.get();
        } finally {
            for (int i = acquired - 1; i >= 0; i--) ordered.get(i).lock.readLock().unlock();
        }
    }
    private static void collect(IWaveform waveform, Map<Scope, Boolean> scopes) {
        Scope scope = OWNERS.get(waveform);
        if (scope != null) scopes.put(scope, Boolean.TRUE);
        else {
            List<IWaveform> sources = waveform.getReadSources();
            if (sources != null) for (IWaveform source : sources) if (source != null && source != waveform) collect(source, scopes);
        }
    }
}
