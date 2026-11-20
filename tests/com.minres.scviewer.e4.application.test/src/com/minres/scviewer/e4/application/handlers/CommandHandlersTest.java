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
package com.minres.scviewer.e4.application.handlers;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Test;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.DesignBrowser;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class CommandHandlersTest {
    private static class Viewer extends WaveformViewer {
        ISelection selection = StructuredSelection.EMPTY;
        DesignBrowser browser;
        IWaveform[] added;
        boolean insert;
        int searches;
        final IWaveformDb db = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformDb.class},
                (proxy, method, args) -> method.getName().equals("isLoaded") ? true : null);
        @Override public ISelection getSelection() { return selection; }
        @Override public IWaveformDb getDatabase() { return db; }
        @Override public DesignBrowser getDesignBrowser() { return browser; }
        @Override public void addStreamsToList(IWaveform[] waveforms, boolean before) { added = waveforms; insert = before; }
        @Override public void focusTransactionSearch() { searches++; }
    }

    private DesignBrowser browser(Viewer viewer, Object... entries) {
        return new DesignBrowser() {
            @Override public WaveformViewer getActiveWaveformViewerPart() { return viewer; }
            @Override public Object[] getFilteredChildren() { return entries; }
        };
    }

    private IWaveform waveform(WaveformType type) {
        return (IWaveform) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveform.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> type;
                    case "getWidth" -> 1;
                    case "getChildNodes" -> List.of();
                    default -> null;
                });
    }

    private MPart part(Object object) {
        return (MPart) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{MPart.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? object : null);
    }

    private EPartService parts(AtomicReference<MPart> active) {
        return (EPartService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EPartService.class},
                (proxy, method, args) -> method.getName().equals("getActivePart") ? active.get() : null);
    }

    @Test
    public void combineHandlerRoutesToActiveViewerAndHonorsDialogCancellation() {
        var first = new TrackEntry(waveform(WaveformType.SIGNAL), null);
        var second = new TrackEntry(waveform(WaveformType.SIGNAL), null);
        class CombineViewer extends Viewer {
            boolean enabled = true;
            CombineBitsHandler.Request request;
            @Override public List<TrackEntry> selectedBits() { return enabled ? List.of(first, second) : List.of(); }
            @Override public void combineBits(String name, List<TrackEntry> bits) {
                request = new CombineBitsHandler.Request(name, bits);
            }
        }
        CombineViewer viewer = new CombineViewer();
        AtomicReference<MPart> active = new AtomicReference<>();
        var service = parts(active);
        AtomicReference<CombineBitsHandler.Request> answer = new AtomicReference<>();
        CombineBitsHandler handler = new CombineBitsHandler() {
            @Override protected Request requestVector(org.eclipse.swt.widgets.Shell shell, List<TrackEntry> bits) {
                assertEquals(List.of(first, second), bits); return answer.get();
            }
        };
        try {
            assertFalse(handler.canExecute(service)); handler.execute(service, null);
            active.set(part(viewer)); assertTrue(handler.canExecute(service));
            handler.execute(service, null); assertNull(viewer.request);
            answer.set(new CombineBitsHandler.Request("bus", List.of(second, first)));
            handler.execute(service, null); assertEquals(answer.get(), viewer.request);
            viewer.enabled = false; viewer.request = null;
            assertFalse(handler.canExecute(service)); handler.execute(service, null); assertNull(viewer.request);
            active.set(part(new Object())); assertFalse(handler.canExecute(service));
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void groupingHandlersFollowActivePartAndGuardMissingSelection() {
        class GroupViewer extends Viewer {
            boolean enabled;
            String name;
            int ungrouped;
            @Override public boolean canGroupSelectedTracks() { return enabled; }
            @Override public boolean canUngroupSelectedTracks() { return enabled; }
            @Override public void groupSelectedTracks(String value) { name = value; }
            @Override public void ungroupSelectedTracks() { ungrouped++; }
        }
        AtomicReference<MPart> active = new AtomicReference<>();
        EPartService service = parts(active);
        AtomicReference<String> answer = new AtomicReference<>("bus");
        GroupTracksHandler group = new GroupTracksHandler() {
            @Override protected String requestGroupName(org.eclipse.swt.widgets.Shell shell) { return answer.get(); }
        };
        UngroupTracksHandler ungroup = new UngroupTracksHandler();
        assertFalse(group.canExecute(service));
        assertFalse(ungroup.canExecute(service));
        group.execute(service, null); ungroup.execute(service);
        active.set(part("other part"));
        assertFalse(group.canExecute(service));
        GroupViewer first = new GroupViewer(), second = new GroupViewer();
        try {
            active.set(part(first));
            assertFalse(group.canExecute(service));
            group.execute(service, null); ungroup.execute(service);
            assertNull(first.name); assertEquals(0, first.ungrouped);
            first.enabled = true;
            assertTrue(group.canExecute(service)); assertTrue(ungroup.canExecute(service));
            answer.set(null); group.execute(service, null);
            assertNull(first.name);
            second.enabled = true;
            active.set(part(second));
            answer.set("bus"); group.execute(service, null); ungroup.execute(service);
            assertEquals("bus", second.name); assertEquals(1, second.ungrouped);
            assertNull(first.name); assertEquals(0, first.ungrouped);
        } finally { first.closeDatabase(); second.closeDatabase(); }
    }

    @Test
    public void addAllUsesFilteredChildrenWithoutASelectionAndFollowsActiveTab() {
        Viewer first = new Viewer(), second = new Viewer();
        IWaveform a = waveform(WaveformType.STREAM), b = waveform(WaveformType.STREAM);
        first.browser = browser(first, a, "unrelated"); second.browser = browser(second, b);
        AtomicReference<MPart> active = new AtomicReference<>(part(first));
        EPartService service = parts(active);
        AddWaveformHandler handler = new AddWaveformHandler();
        try {
            assertTrue(handler.canExecute("after", "true", service, null));
            handler.execute("after", "true", service, null);
            assertArrayEquals(new IWaveform[]{a}, first.added);
            active.set(part(second));
            handler.execute("after", "true", service, null);
            assertArrayEquals(new IWaveform[]{b}, second.added);
            assertFalse(second.insert);
        } finally { first.closeDatabase(); second.closeDatabase(); }
    }

    @Test
    public void staleBrowserFromPreviousTabCannotReceiveAddCommands() {
        Viewer previous = new Viewer(), activeViewer = new Viewer();
        IWaveform wave = waveform(WaveformType.STREAM);
        activeViewer.browser = browser(previous, wave);
        EPartService service = parts(new AtomicReference<>(part(activeViewer)));
        AddWaveformHandler handler = new AddWaveformHandler();
        try {
            assertFalse(handler.canExecute("after", "true", service, null));
            handler.execute("after", "true", service, null);
            assertNull(previous.added);
            assertNull(activeViewer.added);
        } finally { previous.closeDatabase(); activeViewer.closeDatabase(); }
    }

    @Test
    public void executeRechecksInsertionAndRejectsMissingOrUnrelatedSelection() {
        Viewer viewer = new Viewer();
        IWaveform wave = waveform(WaveformType.STREAM);
        viewer.browser = browser(viewer, wave);
        EPartService service = parts(new AtomicReference<>(part(viewer)));
        AddWaveformHandler handler = new AddWaveformHandler();
        try {
            handler.execute("before", "true", service, null);
            assertNull(viewer.added);
            assertFalse(handler.canExecute("after", "false", service, new StructuredSelection("unrelated")));
            handler.execute("after", "false", service, null);
            assertNull(viewer.added);
            viewer.selection = new StructuredSelection(new TrackEntry(wave, null));
            handler.execute("before", "false", service, new StructuredSelection(wave));
            assertArrayEquals(new IWaveform[]{wave}, viewer.added);
            assertTrue(viewer.insert);
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void searchRevealsExistingControlsOnlyForTransactionTracks() {
        Viewer viewer = new Viewer();
        SearchHandler handler = new SearchHandler();
        MPart part = part(viewer);
        try {
            viewer.selection = new StructuredSelection(new TrackEntry(waveform(WaveformType.SIGNAL), null));
            handler.execute(null, part);
            assertEquals(0, viewer.searches);
            viewer.selection = new StructuredSelection(new TrackEntry(waveform(WaveformType.STREAM), null));
            assertTrue(handler.evaluate(part));
            handler.execute(null, part);
            assertEquals(1, viewer.searches);
            assertFalse(handler.evaluate(null));
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void missingActivePartIsSafeDuringCommandExecution() {
        EPartService service = parts(new AtomicReference<>());
        AddWaveformHandler add = new AddWaveformHandler();
        assertFalse(add.canExecute("after", "true", service, null));
        add.execute("after", "true", service, null);
        new ReloadHandler().execute(null, null, null, service);
        new LoadStoreSettingsHandler().execute("load", null, null, null, service);
        new SearchHandler().execute(null, null);
    }
}
