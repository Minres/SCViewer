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

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

/** Owns session replacement, callback validity, and reload monitoring, without SWT. */
public final class DatabaseSessionManager implements AutoCloseable {
    private final IWaveformDbFactory factory;
    private final Consumer<Runnable> dispatch;
    private final Consumer<IWaveformDb> updated;
    private final Consumer<IWaveformDb> filesChanged;
    private final FileMonitor monitor;
    private final long checkInterval;
    private IWaveformDb database;
    private DatabaseLoadSession session;
    private List<File> files = List.of();
    private IFileChangeListener watcher;
    private IModificationChecker checker;
    private long watchGeneration;
    private boolean loading;
    private boolean reloadEnabled;
    private boolean closed;

    public DatabaseSessionManager(IWaveformDbFactory factory, Consumer<Runnable> dispatch,
            Consumer<IWaveformDb> updated, Consumer<IWaveformDb> filesChanged, long checkInterval) {
        this.factory = factory;
        this.dispatch = dispatch;
        this.updated = updated;
        this.filesChanged = filesChanged;
        this.checkInterval = checkInterval;
        this.monitor = new FileMonitor();
        database = factory.getDatabase();
    }

    /** Controller methods and dispatched callbacks run on the owning thread. */
    public IWaveformDb load(List<File> input, long delay, Consumer<IStatus> completion) {
        if (closed) throw new IllegalStateException("Database sessions are closed");
        stopWatching();
        if (session != null) {
            session.close();
            database = factory.getDatabase();
        }
        files = List.copyOf(input);
        IWaveformDb requested = database;
        requested.setName(files.stream().map(File::getName).collect(java.util.stream.Collectors.joining(",")));
        loading = true;
        session = new DatabaseLoadSession(requested, files, event -> dispatch.accept(() -> {
            if (isCurrent(requested)) updated.accept(requested);
        }), status -> dispatch.accept(() -> {
            if (!isCurrent(requested)) return;
            loading = false;
            completion.accept(status);
            if (isCurrent(requested) && status.isOK()) startWatching();
        }));
        session.schedule(delay);
        return requested;
    }

    public IWaveformDb database() { return database; }

    public boolean isCurrent(IWaveformDb requested) { return !closed && database == requested; }

    public void setReloadEnabled(boolean enabled) {
        reloadEnabled = enabled;
        startWatching();
    }

    public void checkFiles() {
        if (checker != null) checker.check();
    }

    private void stopWatching() {
        ++watchGeneration;
        if (watcher != null) monitor.removeFileChangeListener(watcher);
        watcher = null;
        checker = null;
    }

    private void startWatching() {
        stopWatching();
        if (closed || loading || !reloadEnabled || session == null || !database.isLoaded()) return;
        long generation = watchGeneration;
        IWaveformDb requested = database;
        watcher = changed -> dispatch.accept(() -> {
            if (!isCurrent(requested) || !reloadEnabled || generation != watchGeneration) return;
            stopWatching();
            try { filesChanged.accept(requested); }
            finally {
                // Declining a reload acknowledges the current timestamps and resumes polling.
                if (isCurrent(requested)) startWatching();
            }
        });
        checker = monitor.addFileChangeListener(watcher, files, checkInterval);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        stopWatching();
        monitor.dispose();
        if (session != null) session.close();
        else Job.create("Close Database", progress -> {
            database.close();
            return org.eclipse.core.runtime.Status.OK_STATUS;
        }).schedule();
    }
}
