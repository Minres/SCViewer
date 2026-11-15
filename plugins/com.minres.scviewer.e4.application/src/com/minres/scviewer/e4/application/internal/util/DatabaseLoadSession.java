package com.minres.scviewer.e4.application.internal.util;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.minres.scviewer.database.IWaveformDb;

/** Owns one database and its background load, including cancellation and cleanup. */
public final class DatabaseLoadSession implements AutoCloseable {
    private final IWaveformDb database;
    private final List<File> files;
    private final PropertyChangeListener listener;
    private final Job job;
    private volatile boolean cancelled;
    private boolean closed;

    public DatabaseLoadSession(IWaveformDb database, List<File> files,
            PropertyChangeListener listener, Consumer<IStatus> completion) {
        this.database = database;
        this.files = List.copyOf(files);
        this.listener = event -> {
            if (!cancelled) listener.propertyChange(event);
        };
        database.addPropertyChangeListener(this.listener);
        job = new Job("Load Database") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IStatus result;
                synchronized (DatabaseLoadSession.this) {
                    try {
                        result = load(monitor);
                    } catch (OperationCanceledException e) {
                        result = Status.CANCEL_STATUS;
                    } catch (RuntimeException e) {
                        result = new Status(IStatus.ERROR, "com.minres.scviewer.e4.application",
                                "Cannot load database", e);
                    } finally {
                        monitor.done();
                    }
                }
                if (!cancelled) completion.accept(result);
                return result;
            }
        };
        job.setSystem(true);
    }

    private IStatus load(IProgressMonitor monitor) {
        monitor.beginTask("Load Database", files.size());
        for (File file : files) {
            if (cancelled || monitor.isCanceled()) return Status.CANCEL_STATUS;
            monitor.subTask(file.getName());
            if (!database.load(file)) {
                return new Status(IStatus.ERROR, "com.minres.scviewer.e4.application",
                        "Cannot load " + file.getAbsolutePath());
            }
            monitor.worked(1);
        }
        return cancelled || monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

    public void schedule(long delay) {
        job.schedule(delay);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void close() {
        cancelled = true;
        job.cancel();
        // A loader may not support interruption. Wait for it only on a worker,
        // and close even when the load was cancelled before its job started.
        Job cleanup = new Job("Close Database") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                synchronized (DatabaseLoadSession.this) {
                    if (!closed) {
                        closed = true;
                        database.removePropertyChangeListener(listener);
                        database.close();
                    }
                }
                return Status.OK_STATUS;
            }
        };
        cleanup.setSystem(true);
        cleanup.schedule();
    }
}
