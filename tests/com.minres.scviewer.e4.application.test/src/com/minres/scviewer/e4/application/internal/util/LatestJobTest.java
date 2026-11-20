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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class LatestJobTest {
    private Runnable take(BlockingQueue<Runnable> queue) throws InterruptedException {
        Runnable result = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("Background result was not queued", result);
        return result;
    }

    @Test
    public void olderQueuedResultCannotOverwriteNewSelection() throws Exception {
        BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        List<String> displayed = new ArrayList<>();
        try (LatestJob<String> query = new LatestJob<>(ui::add, displayed::add)) {
            query.schedule("old selection", monitor -> "old");
            Runnable old = take(ui);
            query.schedule("new selection", monitor -> "new");
            Runnable current = take(ui);
            current.run();
            old.run();
            assertEquals(List.of("new"), displayed);
        }
    }

    @Test
    public void clearingAndReselectingInputInvalidatesPendingResults() throws Exception {
        BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        List<String> displayed = new ArrayList<>();
        try (LatestJob<String> query = new LatestJob<>(ui::add, displayed::add)) {
            query.schedule("selection", monitor -> "stream");
            Runnable beforeClear = take(ui);
            query.cancel();
            beforeClear.run();
            assertTrue(displayed.isEmpty());
            query.schedule("same selection", monitor -> "stream");
            take(ui).run();
            assertEquals(List.of("stream"), displayed);
        }
    }

    @Test
    public void closingDiscardsQueuedResultsAndPreventsNewWork() throws Exception {
        BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        List<String> displayed = new ArrayList<>();
        LatestJob<String> query = new LatestJob<>(ui::add, displayed::add);
        query.schedule("selection", monitor -> "stream");
        Runnable queued = take(ui);
        query.close();
        queued.run();
        query.schedule("closed", monitor -> { fail("Disposed query must not run"); return ""; });
        assertTrue(displayed.isEmpty());
    }

    @Test public void additiveRequestsBothPublishAndCloseDiscardsQueuedResults() throws Exception {
        BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        List<String> displayed = new ArrayList<>();
        try (LatestJob<String> jobs = new LatestJob<>(ui::add, displayed::add, false)) {
            jobs.schedule("first vector", monitor -> "first");
            Runnable first = take(ui);
            jobs.schedule("second vector", monitor -> "second");
            Runnable second = take(ui);
            first.run(); second.run();
            assertEquals(List.of("first", "second"), displayed);
            jobs.schedule("stale", monitor -> "stale");
            Runnable stale = take(ui);
            jobs.cancel(); stale.run();
            assertEquals(2, displayed.size());
        }
    }

    @Test public void cancellingJobReachesNestedReadContext() throws Exception {
        BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch stopped = new java.util.concurrent.CountDownLatch(1);
        try (LatestJob<String> query = new LatestJob<>(ui::add, ignored -> fail("Cancelled job published"))) {
            query.schedule("long read", monitor -> {
                entered.countDown();
                try { while (true) com.minres.scviewer.database.ReadCancellation.check(); }
                finally { stopped.countDown(); }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            query.cancel();
            assertTrue(stopped.await(5, TimeUnit.SECONDS));
            assertTrue(ui.isEmpty());
        }
    }
}
