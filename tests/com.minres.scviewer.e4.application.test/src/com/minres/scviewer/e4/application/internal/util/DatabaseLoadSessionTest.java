/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.internal.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.RelationType;

public class DatabaseLoadSessionTest {
    private static class Database extends HierNode implements IWaveformDb {
        final List<File> loadedFiles = new ArrayList<>();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        boolean block;
        boolean success = true;
        int listenersAtClose;

        @Override public boolean load(File file) {
            loadedFiles.add(file);
            entered.countDown();
            if (block) {
                boolean done = false;
                while (!done) {
                    try { done = release.await(5, TimeUnit.SECONDS); }
                    catch (InterruptedException e) { /* Simulate a loader that cannot be interrupted. */ }
                }
            }
            pcs.firePropertyChange(IHierNode.LOADING_FINISHED, null, success);
            return success;
        }
        @Override public void close() {
            listenersAtClose = pcs.getPropertyChangeListeners().length;
            closed.countDown();
        }
        @Override public boolean isLoaded() { return !loadedFiles.isEmpty(); }
        @Override public long getMaxTime() { return 0; }
        @Override public IWaveform getStreamByName(String name) { return null; }
        @Override public List<IWaveform> getAllWaves() { return List.of(); }
        @Override public List<RelationType> getAllRelationTypes() { return List.of(); }
    }

    @Test(timeout = 10000)
    public void closeDuringLoadDoesNotWaitOrPublishAndSkipsRemainingFiles() throws Exception {
        Database db = new Database();
        db.block = true;
        AtomicInteger notifications = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        DatabaseLoadSession session = new DatabaseLoadSession(db,
                List.of(new File("first"), new File("second")),
                event -> notifications.incrementAndGet(), status -> completions.incrementAndGet());
        try {
            session.schedule(0);
            assertTrue(db.entered.await(5, TimeUnit.SECONDS));
            session.close();
            assertTrue(session.isCancelled());
            assertEquals("Database must remain open while its loader is using it", 1, db.closed.getCount());
            db.release.countDown();
            assertTrue(db.closed.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(new File("first")), db.loadedFiles);
            assertEquals(0, notifications.get());
            assertEquals(0, completions.get());
            assertEquals(0, db.listenersAtClose);
        } finally {
            db.release.countDown();
            session.close();
        }
    }

    @Test(timeout = 10000)
    public void closeBeforeScheduledLoadStillReleasesDatabase() throws Exception {
        Database db = new Database();
        DatabaseLoadSession session = new DatabaseLoadSession(db, List.of(new File("first")),
                event -> fail("Cancelled load must not notify"), status -> fail("Cancelled load must not finish"));
        session.schedule(60000);
        session.close();
        assertTrue(db.closed.await(5, TimeUnit.SECONDS));
        assertTrue(db.loadedFiles.isEmpty());
        assertEquals(0, db.listenersAtClose);
    }

    @Test(timeout = 10000)
    public void fileListIsSnapshottedAndFirstFailureStopsTheLoad() throws Exception {
        Database db = new Database();
        db.success = false;
        List<File> files = new ArrayList<>(List.of(new File("first"), new File("second")));
        AtomicReference<IStatus> result = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        DatabaseLoadSession session = new DatabaseLoadSession(db, files, event -> {}, status -> {
            result.set(status);
            completed.countDown();
        });
        try {
            files.clear();
            session.schedule(0);
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(new File("first")), db.loadedFiles);
            assertEquals(IStatus.ERROR, result.get().getSeverity());
            assertTrue(result.get().getMessage().contains("first"));
        } finally { session.close(); }
    }
}
