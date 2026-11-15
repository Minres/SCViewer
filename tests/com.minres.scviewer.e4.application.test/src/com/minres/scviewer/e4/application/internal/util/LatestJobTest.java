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
}
