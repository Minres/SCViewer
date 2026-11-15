package com.minres.scviewer.e4.application.internal.util;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.Test;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.RelationType;

public class DatabaseSessionManagerTest {
    private static class Database extends HierNode implements IWaveformDb {
        boolean loaded;
        final CountDownLatch closed = new CountDownLatch(1);
        @Override public boolean load(File file) { loaded = true; pcs.firePropertyChange(IHierNode.LOADING_FINISHED, null, true); return true; }
        @Override public void close() { closed.countDown(); }
        @Override public boolean isLoaded() { return loaded; }
        @Override public long getMaxTime() { return 0; }
        @Override public IWaveform getStreamByName(String name) { return null; }
        @Override public List<IWaveform> getAllWaves() { return List.of(); }
        @Override public List<RelationType> getAllRelationTypes() { return List.of(); }
    }

    private void runUntil(BlockingQueue<Runnable> queue, BooleanSupplier finished) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!finished.getAsBoolean()) {
            Runnable task = queue.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull("Controller callback did not arrive", task);
            task.run();
        }
    }

    @Test
    public void declinedChangesResumeMonitoringAndDisabledQueuedChangesAreIgnored() throws Exception {
        Path file = Files.createTempFile("session-watch", ".tmp");
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        AtomicInteger completions = new AtomicInteger(), prompts = new AtomicInteger();
        Database database = new Database();
        try (DatabaseSessionManager sessions = new DatabaseSessionManager(() -> database, queue::add,
                db -> {}, db -> prompts.incrementAndGet(), 60000)) {
            sessions.setReloadEnabled(true);
            sessions.load(List.of(file.toFile()), 0, result -> completions.incrementAndGet());
            runUntil(queue, () -> completions.get() == 1);
            change(file); sessions.checkFiles();
            runUntil(queue, () -> prompts.get() == 1);
            sessions.checkFiles();
            assertTrue("Declined change should be acknowledged", queue.isEmpty());
            change(file); sessions.checkFiles();
            runUntil(queue, () -> prompts.get() == 2);
            change(file); sessions.checkFiles();
            sessions.setReloadEnabled(false);
            Runnable pending = queue.poll(5, TimeUnit.SECONDS);
            assertNotNull(pending); pending.run();
            assertEquals(2, prompts.get());
            sessions.setReloadEnabled(true);
            change(file); sessions.checkFiles();
            runUntil(queue, () -> prompts.get() == 3);
        } finally { Files.deleteIfExists(file); }
        assertTrue(database.closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void replacementRejectsQueuedResultsFromOldDatabase() throws Exception {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        Database first = new Database(), second = new Database();
        AtomicInteger created = new AtomicInteger(), oldCompleted = new AtomicInteger(), newCompleted = new AtomicInteger();
        List<IWaveformDb> updates = new java.util.ArrayList<>();
        try (DatabaseSessionManager sessions = new DatabaseSessionManager(
                () -> created.getAndIncrement() == 0 ? first : second, queue::add, updates::add, db -> {}, 60000)) {
            sessions.load(List.of(new File("first")), 0, result -> oldCompleted.incrementAndGet());
            Runnable oldUpdate = queue.poll(5, TimeUnit.SECONDS);
            assertNotNull(oldUpdate);
            sessions.load(List.of(new File("second")), 0, result -> newCompleted.incrementAndGet());
            oldUpdate.run();
            runUntil(queue, () -> newCompleted.get() == 1);
            assertEquals(0, oldCompleted.get());
            assertFalse(sessions.isCurrent(first));
            assertTrue(sessions.isCurrent(second));
            assertTrue(updates.stream().allMatch(db -> db == second));
        }
        assertTrue(first.closed.await(5, TimeUnit.SECONDS));
        assertTrue(second.closed.await(5, TimeUnit.SECONDS));
    }

    private void change(Path file) throws Exception {
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10000));
    }
}
