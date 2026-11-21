/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

import com.minres.scviewer.database.CombinedBitVectorWaveform;
import com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ui.BitWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class EditTrackTextHandlerTest {
    private static class Viewer extends WaveformViewer {
        ISelection selection = StructuredSelection.EMPTY;
        int updates;
        @Override public ISelection getSelection() { return selection; }
        @Override public void update() { updates++; }
    }

    private static class Handler extends WaveformDoubleClickTextHandler {
        String answer;
        int requests;
        @Override protected String requestText(Shell shell, TrackEntry track) { requests++; return answer; }
    }

    private MPart part(Object viewer) {
        return (MPart) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{MPart.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? viewer : null);
    }

    private EPartService service(MPart part) {
        return (EPartService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EPartService.class},
                (proxy, method, args) -> method.getName().equals("getActivePart") ? part : null);
    }

    private EmptyWaveform signal(int width) {
        return new EmptyWaveform("recorded") {
            @Override public WaveformType getType() { return WaveformType.SIGNAL; }
            @Override public int getWidth() { return width; }
            @Override public IEventList getEvents() { throw new AssertionError("Editing text must not load events"); }
        };
    }

    private CombinedBitVectorWaveform combined() {
        var source = signal(2);
        return new CombinedBitVectorWaveform("combined", List.of(new SourceBit(source, 1), new SourceBit(source, 0)));
    }

    @Test public void enablesOnlyOneGroupOrCombinedVector() {
        Viewer viewer = new Viewer();
        MPart part = part(viewer);
        Handler handler = new Handler();
        assertFalse(handler.canExecute(null));
        assertFalse(handler.canExecute(part("other part")));
        TrackEntry group = TrackEntry.group("group", List.of(), null);
        TrackEntry combined = new TrackEntry(combined(), null);
        for (TrackEntry track : List.of(group, combined)) {
            viewer.selection = new StructuredSelection(track);
            assertTrue(handler.canExecute(part));
            track.setExpanded(false);
            assertTrue(handler.canExecute(part));
            track.setExpanded(true);
            assertTrue(handler.canExecute(part));
        }
        viewer.selection = new StructuredSelection(List.of(group, combined));
        assertFalse(handler.canExecute(part));
        handler.execute(null, service(part));
        assertEquals(0, handler.requests);
    }

    @Test public void rejectsRecordedSignalsBitsSeparatorsAndUnrelatedSelections() {
        Viewer viewer = new Viewer();
        MPart part = part(viewer);
        Handler handler = new Handler();
        var stream = new EmptyWaveform("stream") {
            @Override public WaveformType getType() { return WaveformType.STREAM; }
        };
        for (Object value : List.of(new TrackEntry(signal(1), null), new TrackEntry(signal(4), null),
                new TrackEntry(new BitWaveform(signal(4), 2), null),
                new TrackEntry(new BitWaveform(combined(), 0), null),
                new TrackEntry(new EmptyWaveform("separator"), null), new TrackEntry(stream, null),
                new TrackEntry(), "other selection")) {
            viewer.selection = new StructuredSelection(value);
            assertFalse(handler.canExecute(part));
            handler.execute(null, service(part));
        }
        viewer.selection = StructuredSelection.EMPTY;
        assertFalse(handler.canExecute(part));
        handler.execute(null, service(part));
        handler.execute(null, service(null));
        assertEquals(0, handler.requests);
        assertEquals(0, viewer.updates);
    }

    @Test public void rejectsBlankCombinedVectorNamesSoSettingsRemainValid() {
        Viewer viewer = new Viewer();
        MPart part = part(viewer);
        var vector = combined();
        viewer.selection = new StructuredSelection(new TrackEntry(vector, null));
        Handler handler = new Handler();
        for (String text : List.of("", " ", "\t")) {
            handler.answer = text;
            handler.execute(null, service(part));
            assertEquals("combined", vector.getName());
        }
        assertEquals(0, viewer.updates);
    }

    @Test public void renamesEditableTracksWithoutChangingIdentityOrExpansionAndHonorsCancellation() {
        Viewer viewer = new Viewer();
        MPart part = part(viewer);
        Handler handler = new Handler();
        TrackEntry group = TrackEntry.group("group", List.of(), null);
        var vector = combined();
        TrackEntry combined = new TrackEntry(vector, null);
        String identity = vector.getFullName();
        for (TrackEntry track : List.of(group, combined)) {
            viewer.selection = new StructuredSelection(track);
            var state = track.hierState;
            handler.answer = null;
            String oldName = track.waveform.getName();
            handler.execute(null, service(part));
            assertEquals(oldName, track.waveform.getName());
            handler.answer = "new label";
            handler.execute(null, service(part));
            assertEquals("new label", track.waveform.getName());
            assertEquals(state, track.hierState);
        }
        assertEquals(identity, vector.getFullName());
        assertEquals("new label", vector.getDisplayName());
        assertEquals("recorded", vector.getSources().get(0).waveform().getName());
        assertEquals(2, viewer.updates);
    }
}
