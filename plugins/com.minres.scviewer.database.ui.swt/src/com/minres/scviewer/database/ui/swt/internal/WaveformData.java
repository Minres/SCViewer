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
package com.minres.scviewer.database.ui.swt.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import com.minres.scviewer.database.*;
import com.minres.scviewer.database.ui.BitWaveform;
import com.minres.scviewer.database.tx.TxIntervalIndex;

/** UI-owned snapshots of recording data. Painting never triggers a lazy read. */
final class WaveformData implements AutoCloseable {
    private record Request(IWaveform waveform, long from, long to, long step) { }
    private record Snapshot(IEventList events, int rows, TxIntervalIndex intervals) { }
    private static final IEventList EMPTY = new EventList();
    private final Map<Request, Snapshot> values = new LinkedHashMap<>(32, .75f, true);
    private final Map<Request, Job> jobs = new LinkedHashMap<>();
    private final Consumer<Runnable> dispatch;
    private final Runnable changed;
    private volatile boolean closed;
    WaveformData(Consumer<Runnable> dispatch, Runnable changed) { this.dispatch = dispatch; this.changed = changed; }

    private static boolean derived(IWaveform waveform) {
        while (waveform instanceof BitWaveform bit) waveform = bit.getSource();
        return waveform instanceof CombinedBitVectorWaveform;
    }
    IEventList events(IWaveform waveform, long from, long to, long step) {
        Snapshot value = request(waveform, from, to, step);
        return value == null ? EMPTY : value.events();
    }
    int rows(IWaveform waveform) {
        if (!waveform.getType().isTransaction()) return 1;
        Snapshot value = request(waveform, 0, -1L, 0);
        return value == null ? 1 : Math.max(1, value.rows());
    }
    TxIntervalIndex intervals(IWaveform waveform) {
        Snapshot value = request(waveform, 0, -1L, 0);
        return value == null ? null : value.intervals();
    }
    private Snapshot request(IWaveform waveform, long from, long to, long step) {
        if (closed) return null;
        boolean sample = derived(waveform);
        if (!sample) { from = 0; to = -1L; step = 0; }
        // Metadata callers do not request an unbounded derived timeline.
        else if (step == 0) return null;
        Request request = new Request(waveform, from, to, Math.max(0, step));
        Snapshot value = values.get(request);
        if (value != null) return value;
        if (!jobs.containsKey(request)) {
            // Supersede obsolete ranges and cursor samples independently.
            if (sample) {
                var stale = jobs.entrySet().iterator();
                while (stale.hasNext()) {
                    var entry = stale.next();
                    if (entry.getKey().waveform() == waveform
                            && (entry.getKey().from() == entry.getKey().to()) == (request.from() == request.to())) {
                        entry.getValue().cancel(); stale.remove();
                    }
                }
            }
            Job job = new Job("Prepare waveform") {
                @Override protected IStatus run(IProgressMonitor monitor) {
                    try {
                        Snapshot loaded = ReadCancellation.with(() -> closed || monitor.isCanceled(),
                            () -> WaveformAccess.read(waveform, () -> {
                                IEventList events = waveform.getEvents();
                                IEventList ready = sample ? sample(events, request) : events;
                                int rows = waveform.getRowCount();
                                return new Snapshot(ready, rows, waveform.getType().isTransaction() ? new TxIntervalIndex(ready) : null);
                            }));
                        if (closed || monitor.isCanceled()) return Status.CANCEL_STATUS;
                        dispatch.accept(() -> {
                            if (closed || jobs.get(request) != this) return;
                            jobs.remove(request);
                            values.put(request, loaded);
                            if (sample) {
                                long count = values.keySet().stream().filter(key -> key.waveform() == waveform).count();
                                var old = values.keySet().iterator();
                                while (count > 8 && old.hasNext()) {
                                    if (old.next().waveform() == waveform) { old.remove(); count--; }
                                }
                            }
                            changed.run();
                        });
                        return Status.OK_STATUS;
                    } catch (java.util.concurrent.CancellationException e) {
                        dispatch.accept(() -> { if (!closed && jobs.get(request) == this) jobs.remove(request); });
                        return Status.CANCEL_STATUS;
                    } catch (RuntimeException e) {
                        dispatch.accept(() -> {
                            if (!closed && jobs.get(request) == this) { jobs.remove(request); values.put(request, new Snapshot(EMPTY, 1, null)); changed.run(); }
                        });
                        return new Status(IStatus.ERROR, "com.minres.scviewer.database.ui.swt", "Cannot prepare waveform", e);
                    } finally { monitor.done(); }
                }
            };
            jobs.put(request, job); job.setSystem(true); job.schedule();
        }
        return null;
    }
    /** At most a few samples per pixel, preserving boundaries and dense-change markers. */
    private static IEventList sample(IEventList input, Request request) {
        EventList result = new EventList();
        add(result, input.floorEntry(request.from()));
        for (long time = request.from(); Long.compareUnsigned(time, request.to()) <= 0;) {
            ReadCancellation.check();
            add(result, input.floorEntry(time));
            add(result, input.ceilingEntry(time));
            long next = time + Math.max(1, request.step());
            if (Long.compareUnsigned(next, time) <= 0) break;
            time = next;
        }
        add(result, input.floorEntry(request.to()));
        return result;
    }
    private static void add(EventList result, EventEntry entry) {
        if (entry != null && entry.events != null && !result.containsKey(entry.timestamp))
            for (IEvent event : entry.events) result.put(entry.timestamp, event);
    }
    void retain(java.util.Set<IWaveform> waveforms) {
        values.keySet().removeIf(request -> !waveforms.contains(request.waveform()));
        var pending = jobs.entrySet().iterator();
        while (pending.hasNext()) {
            var entry = pending.next();
            if (!waveforms.contains(entry.getKey().waveform())) { entry.getValue().cancel(); pending.remove(); }
        }
    }
    @Override public void close() {
        closed = true;
        jobs.values().forEach(Job::cancel); jobs.clear(); values.clear();
    }
}
