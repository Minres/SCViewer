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
package com.minres.scviewer.database.ui;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import com.minres.scviewer.database.ReadCancellation;
import com.minres.scviewer.database.WaveformAccess;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxRelation;

/** UI-owned, bounded snapshots. Accessors never call recording code on the UI thread. */
public final class AsyncTransactionData implements AutoCloseable {
    public record Snapshot(List<ITxAttribute> attributes, List<ITxRelation> incoming, List<ITxRelation> outgoing) { }
    private static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), List.of());
    private final Map<ITx, Snapshot> values = new IdentityHashMap<>();
    private final Map<ITx, Job> jobs = new IdentityHashMap<>();
    private final java.util.ArrayDeque<ITx> order = new java.util.ArrayDeque<>();
    private final boolean relations;
    private final Consumer<Runnable> dispatch;
    private final Consumer<ITx> changed;
    private volatile boolean closed;
    public AsyncTransactionData(Consumer<Runnable> dispatch, Consumer<ITx> changed) {
        this(dispatch, changed, true);
    }
    public AsyncTransactionData(Consumer<Runnable> dispatch, Consumer<ITx> changed, boolean relations) {
        this.relations = relations;
        this.dispatch = dispatch; this.changed = changed;
    }
    public Snapshot get(ITx tx) {
        if (closed || tx == null) return EMPTY;
        Snapshot value = values.get(tx);
        if (value != null) return value;
        if (!jobs.containsKey(tx)) {
            Job job = new Job("Load transaction details") {
                @Override protected IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                    try {
                        Snapshot loaded = ReadCancellation.with(() -> closed || monitor.isCanceled(),
                            () -> WaveformAccess.read(tx.getStream(), () -> new Snapshot(List.copyOf(tx.getAttributes()),
                                    relations ? List.copyOf(tx.getIncomingRelations()) : List.of(),
                                    relations ? List.copyOf(tx.getOutgoingRelations()) : List.of())));
                        dispatch.accept(() -> {
                            if (closed || jobs.get(tx) != this) return;
                            jobs.remove(tx);
                            if (values.size() >= 512) values.remove(order.removeFirst());
                            values.put(tx, loaded); order.addLast(tx);
                            changed.accept(tx);
                        });
                        return Status.OK_STATUS;
                    } catch (java.util.concurrent.CancellationException e) {
                        dispatch.accept(() -> { if (!closed && jobs.get(tx) == this) jobs.remove(tx); });
                        return Status.CANCEL_STATUS;
                    } catch (RuntimeException e) {
                        dispatch.accept(() -> {
                            if (!closed && jobs.get(tx) == this) { jobs.remove(tx); values.put(tx, EMPTY); order.addLast(tx); changed.accept(tx); }
                        });
                        return new Status(IStatus.ERROR, "com.minres.scviewer.database.ui.swt", "Cannot load transaction details", e);
                    } finally { monitor.done(); }
                }
            };
            jobs.put(tx, job); job.setSystem(true); job.schedule();
        }
        return EMPTY;
    }
    public boolean isReady(ITx tx) { return values.containsKey(tx); }
    @Override public void close() {
        closed = true;
        jobs.values().forEach(Job::cancel);
        jobs.clear(); values.clear(); order.clear();
    }
}
