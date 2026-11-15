package com.minres.scviewer.e4.application.internal.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/** Publishes only the latest request, including when older results are already queued. */
public final class LatestJob<T> implements AutoCloseable {
    private final AtomicLong generation = new AtomicLong();
    private final Consumer<Runnable> dispatch;
    private final Consumer<T> publish;
    private Job job;
    private volatile boolean closed;

    public LatestJob(Consumer<Runnable> dispatch, Consumer<T> publish) {
        this.dispatch = dispatch;
        this.publish = publish;
    }

    /** Called by the owning UI thread, as are cancel() and close(). */
    public void schedule(String name, Function<IProgressMonitor, T> query) {
        if (closed) return;
        cancel();
        final long request = generation.get();
        job = new Job(name) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) return Status.CANCEL_STATUS;
                T result = query.apply(monitor);
                if (monitor.isCanceled() || closed || generation.get() != request) return Status.CANCEL_STATUS;
                dispatch.accept(() -> {
                    if (!closed && generation.get() == request) publish.accept(result);
                });
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    public void cancel() {
        generation.incrementAndGet();
        if (job != null) job.cancel();
    }

    @Override
    public void close() {
        closed = true;
        cancel();
    }
}
