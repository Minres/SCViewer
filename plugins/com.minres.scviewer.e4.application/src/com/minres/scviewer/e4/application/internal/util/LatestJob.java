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

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.minres.scviewer.database.ReadCancellation;

/** Owns background requests and rejects stale results, including queued UI callbacks. */
public final class LatestJob<T> implements AutoCloseable {
    private final AtomicLong generation = new AtomicLong();
    private final Consumer<Runnable> dispatch;
    private final Consumer<T> publish;
    private final Set<Job> jobs = ConcurrentHashMap.newKeySet();
    private final boolean latestOnly;
    private volatile boolean closed;

    public LatestJob(Consumer<Runnable> dispatch, Consumer<T> publish) {
        this(dispatch, publish, true);
    }

    /** latestOnly=false preserves additive commands until cancel/close. */
    public LatestJob(Consumer<Runnable> dispatch, Consumer<T> publish, boolean latestOnly) {
        this.latestOnly = latestOnly;
        this.dispatch = dispatch;
        this.publish = publish;
    }

    /** Called by the owning UI thread, as are cancel() and close(). */
    public void schedule(String name, Function<IProgressMonitor, T> query) {
        if (closed) return;
        if (latestOnly) cancel();
        final long request = generation.get();
        Job job = new Job(name) {
            @Override protected IStatus run(IProgressMonitor monitor) {
                try {
                    T result = ReadCancellation.with(
                            () -> monitor.isCanceled() || closed || generation.get() != request,
                            () -> query.apply(monitor));
                    if (monitor.isCanceled() || closed || generation.get() != request) return Status.CANCEL_STATUS;
                    dispatch.accept(() -> {
                        if (result != null && !closed && generation.get() == request) publish.accept(result);
                    });
                    return Status.OK_STATUS;
                } catch (CancellationException | OperationCanceledException e) {
                    return Status.CANCEL_STATUS;
                } finally { jobs.remove(this); monitor.done(); }
            }
        };
        jobs.add(job);
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override public void done(IJobChangeEvent event) { jobs.remove(event.getJob()); }
        });
        job.setSystem(true);
        job.schedule();
    }

    public void cancel() {
        generation.incrementAndGet();
        for (Job job : jobs) job.cancel();
    }

    @Override public void close() {
        closed = true;
        cancel();
    }
}
