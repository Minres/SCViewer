/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.internal.status;

import org.eclipse.core.runtime.NullProgressMonitor;

/** Per-job cancellation and progress state. Only immutable snapshots reach SWT. */
public final class JobProgress extends NullProgressMonitor {
    public record Snapshot(String name, int total, double work, boolean done) { }
    private final Runnable changed;
    private String name = "";
    private int total;
    private double work;
    private boolean done;
    public JobProgress(Runnable changed) { this.changed = changed; }
    @Override public void beginTask(String name, int totalWork) {
        synchronized (this) { this.name = name; total = totalWork; work = 0; done = false; }
        changed.run();
    }
    @Override public void setTaskName(String name) {
        synchronized (this) { this.name = name; }
        changed.run();
    }
    @Override public void subTask(String name) { setTaskName(name); }
    @Override public void worked(int work) { internalWorked(work); }
    @Override public void internalWorked(double amount) {
        synchronized (this) { work += amount; }
        changed.run();
    }
    @Override public void done() {
        synchronized (this) { done = true; }
        changed.run();
    }
    public synchronized Snapshot snapshot() { return new Snapshot(name, total, work, done); }
}
