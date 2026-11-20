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
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import com.minres.scviewer.database.*;

public class ThreadingTest {
    @Test public void registryPublishesEveryNameAndOneIdentity() throws Exception {
        String prefix = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<List<RelationType>>> results = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                int id = thread;
                results.add(pool.submit(() -> {
                    start.await();
                    List<RelationType> values = new ArrayList<>();
                    for (int i = 0; i < 2000; i++) {
                        values.add(RelationTypeFactory.create(prefix + ":" + id + ":" + i));
                        assertSame(RelationTypeFactory.create(prefix), RelationTypeFactory.create(prefix));
                    }
                    return values;
                }));
            }
            start.countDown();
            for (int thread = 0; thread < results.size(); thread++) {
                List<RelationType> values = results.get(thread).get(10, TimeUnit.SECONDS);
                for (int i = 0; i < values.size(); i++) assertSame(values.get(i), RelationTypeFactory.create(prefix + ":" + thread + ":" + i));
            }
        } finally { pool.shutdownNow(); }
    }

    @Test public void hierarchyReturnsStableSnapshotsWithInitializedParents() {
        HierNode root = new HierNode("root");
        HierNode first = new HierNode("first");
        root.addChild(first);
        List<IHierNode> snapshot = root.getChildNodes();
        root.addChild(new HierNode("second"));
        assertEquals(List.of(first), snapshot);
        assertSame(root, snapshot.get(0).getParent());
        assertEquals(2, root.getChildNodes().size());
    }

    @Test public void closureWaitsForReadersAndRejectsStaleReferences() throws Exception {
        WaveformAccess.Scope scope = new WaveformAccess.Scope();
        EmptyWaveform wave = new EmptyWaveform("wave");
        scope.register(List.of(wave));
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean disposed = new AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> reader = pool.submit(() -> WaveformAccess.read(wave, () -> {
                entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                assertFalse(disposed.get());
                return null;
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            CountDownLatch closing = new CountDownLatch(1);
            Future<?> close = pool.submit(() -> { closing.countDown(); scope.close(() -> disposed.set(true)); });
            assertTrue(closing.await(5, TimeUnit.SECONDS));
            Future<?> rejected = pool.submit(() -> {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (System.nanoTime() < deadline) {
                    try { WaveformAccess.read(wave, () -> null); }
                    catch (CancellationException expected) { return; }
                    Thread.yield();
                }
                fail("New readers were admitted during closure");
            });
            rejected.get(5, TimeUnit.SECONDS);
            assertFalse(disposed.get());
            release.countDown();
            reader.get(5, TimeUnit.SECONDS); close.get(5, TimeUnit.SECONDS);
            assertTrue(disposed.get());
            try { WaveformAccess.read(wave, () -> null); fail("Read a closed recording"); }
            catch (CancellationException expected) { }
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test public void cancelledLazyInitializationCanRetry() {
        LazyValue<String> value = new LazyValue<>();
        try { value.get(() -> { throw new CancellationException(); }); fail(); }
        catch (CancellationException expected) { }
        assertEquals("complete", value.get(() -> "complete"));
    }

    @Test public void lazyWaiterCancelsWithoutBlockingOrCancellingOwner() throws Exception {
        LazyValue<String> value = new LazyValue<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        CountDownLatch waiterWaiting = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger checks = new java.util.concurrent.atomic.AtomicInteger();
        try {
            Future<String> owner = pool.submit(() -> value.get(() -> {
                entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return "complete";
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<String> waiter = pool.submit(() -> ReadCancellation.with(() -> {
                if (checks.incrementAndGet() >= 2) waiterWaiting.countDown();
                return cancelled.get();
            }, () -> value.get(() -> "wrong")));
            assertTrue("Waiter never reached cancellable lazy wait", waiterWaiting.await(5, TimeUnit.SECONDS));
            cancelled.set(true);
            try { waiter.get(2, TimeUnit.SECONDS); fail(); }
            catch (ExecutionException expected) { assertTrue(expected.getCause() instanceof CancellationException); }
            assertFalse(owner.isDone());
            release.countDown();
            assertEquals("complete", owner.get(5, TimeUnit.SECONDS));
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
