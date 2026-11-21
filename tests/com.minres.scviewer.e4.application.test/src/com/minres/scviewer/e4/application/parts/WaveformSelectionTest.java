/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Test;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformSelection;

public class WaveformSelectionTest {
    @Test
    public void transactionAndGeneratorTrackAreIndependentOfElementOrder() {
        EmptyWaveform stream = new EmptyWaveform("stream"), generator = new EmptyWaveform("generator");
        ITx tx = (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class},
                (p, m, a) -> m.getName().equals("getStream") ? stream : generator);
        TrackEntry track = new TrackEntry(generator, null);
        WaveformSelection selection = WaveformSelection.from(new StructuredSelection(new Object[]{track, tx}));
        assertSame(tx, selection.transaction());
        assertSame(track, selection.track());
        assertSame(tx, selection.toSelection().getFirstElement());
        assertSame(track, WaveformSelection.from(selection.toSelection()).track());
        TrackEntry unrelated = new TrackEntry(new EmptyWaveform("other"), null);
        assertNull(new WaveformSelection(tx, List.of(unrelated)).track());
    }

    @Test
    public void emptyAndTrackOnlySelectionsHaveNoStaleTransaction() {
        assertNull(WaveformSelection.from(null).transaction());
        assertTrue(WaveformSelection.from(StructuredSelection.EMPTY).toSelection().isEmpty());
        TrackEntry track = new TrackEntry(new EmptyWaveform("stream"), null);
        WaveformSelection selection = WaveformSelection.from(new StructuredSelection(track));
        assertNull(selection.transaction());
        assertSame(track, selection.track());
    }
}
