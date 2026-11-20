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
package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.ui.IWaveformStyleProvider;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;

public class WaveformScrollTest {
    private Display display;
    private Shell shell;
    private IWaveformView view;
    private Canvas canvas;
    private ScrolledComposite names;
    private ScrolledComposite values;

    @Before
    public void createControls() {
        assumeTrue("GTK integration tests require DISPLAY", !"gtk".equals(SWT.getPlatform())
                || System.getenv("DISPLAY") != null);
        display = new Display();
        shell = new Shell(display);
        shell.setSize(1000, 500);
        IWaveformStyleProvider styles = new IWaveformStyleProvider() {
            @Override public Font getNameFont() { return display.getSystemFont(); }
            @Override public Font getNameFontHighlite() { return display.getSystemFont(); }
            @Override public int getTrackHeight() { return 20; }
            @Override public Color getColor(WaveformColors type) { return display.getSystemColor(SWT.COLOR_BLACK); }
            @Override public Color[] computeColor(String name) { return new Color[]{getColor(null)}; }
        };
        view = new WaveformViewFactory().createPanel(shell, styles);
        canvas = (Canvas) view.getWaveformControl();
        names = (ScrolledComposite) view.getNameControl().getParent();
        values = (ScrolledComposite) view.getValueControl().getParent();
        shell.layout(true, true);
        drainEvents();
    }

    @After
    public void disposeControls() {
        if (display == null) return;
        try {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            drainEvents();
        } finally { display.dispose(); }
    }

    private List<TrackEntry> tracks(int count) {
        List<TrackEntry> tracks = new ArrayList<>();
        for (int i = 0; i < count; i++) tracks.add(new TrackEntry(new EmptyWaveform("track " + i), null));
        return tracks;
    }

    private void drainEvents() {
        int events = 0;
        while (display.readAndDispatch()) assertTrue("Layout must converge", ++events < 1000);
    }

    private Object invokeView(String method, Class<?>[] types, Object... arguments) {
        try {
            var target = view.getClass().getDeclaredMethod(method, types);
            target.setAccessible(true);
            return target.invoke(view, arguments);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private Object dropLocation(int y) {
        return invokeView("streamDropLocationAt", new Class<?>[]{int.class}, y);
    }

    private void moveTracks(List<TrackEntry> tracks, Object location) {
        invokeView("moveTracks", new Class<?>[]{List.class, location.getClass()}, tracks, location);
        drainEvents();
    }

    private void assertInsertionLine(int y, boolean expected) {
        Canvas namesCanvas = (Canvas) view.getNameControl();
        var bounds = namesCanvas.getClientArea();
        Image image = new Image(display, bounds.width, bounds.height);
        GC gc = new GC(image);
        try {
            gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            gc.fillRectangle(bounds);
            Event paint = new Event();
            paint.gc = gc;
            namesCanvas.notifyListeners(SWT.Paint, paint);
            var data = image.getImageData();
            assertEquals(expected, display.getSystemColor(SWT.COLOR_RED).getRGB()
                    .equals(data.palette.getRGB(data.getPixel(bounds.width - 2, y))));
        } finally { gc.dispose(); image.dispose(); }
    }

    private WaveformViewer compositionViewer(java.util.function.Consumer<Runnable> dispatch) throws Exception {
        WaveformViewer viewer = new WaveformViewer();
        var pane = WaveformViewer.class.getDeclaredField("waveformPane");
        pane.setAccessible(true); pane.set(viewer, view);
        var synchronizer = WaveformViewer.class.getDeclaredField("sync");
        synchronizer.setAccessible(true);
        synchronizer.set(viewer, new org.eclipse.e4.ui.di.UISynchronize() {
            @Override public void asyncExec(Runnable runnable) { dispatch.accept(runnable); }
            @Override public void syncExec(Runnable runnable) { display.syncExec(runnable); }
            @Override protected boolean isUIThread(Thread thread) { return thread == display.getThread(); }
            @Override protected void showBusyWhile(Runnable runnable) { runnable.run(); }
            @Override protected boolean dispatchEvents() { return display.readAndDispatch(); }
        });
        return viewer;
    }

    private List<TrackEntry> scalarInputs() {
        var a = new BitVectorExpansionTest.Signal("a", 1);
        var b = new BitVectorExpansionTest.Signal("b", 1);
        a.events.put(0, com.minres.scviewer.database.BitVector.fromString(1, "0"));
        b.events.put(0, com.minres.scviewer.database.BitVector.fromString(1, "1"));
        TrackEntry first = view.addWaveform(a, -1), second = view.addWaveform(b, -1);
        view.update();
        return List.of(first, second);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            drainEvents();
            if (System.nanoTime() > deadline) fail("Timed out waiting for the combine job");
            Thread.sleep(5);
        }
    }

    @Test
    public void combinesInRequestedOrderWithoutRemovingSourcesAndSupportsExpansion() throws Exception {
        var inputs = scalarInputs();
        TrackEntry tail = view.addWaveform(new EmptyWaveform("tail"), -1);
        view.setSelection(new StructuredSelection(List.of(inputs.get(1), inputs.get(0))));
        WaveformViewer viewer = compositionViewer(display::asyncExec);
        try {
            assertEquals(inputs, viewer.selectedBits());
            assertTrue(viewer.canCombineSelectedBits());
            viewer.combineBits("bus", List.of(inputs.get(1), inputs.get(0)));
            waitUntil(() -> view.getStreamList().size() == 4);
            TrackEntry bus = view.getStreamList().get(2);
            assertEquals("bus", bus.waveform.getDisplayName());
            assertEquals("10", bus.waveform.getEventsBeforeTime(0)[0].toString());
            assertEquals(List.of(inputs.get(0), inputs.get(1), bus, tail), view.getStreamList());
            assertSame(bus, ((StructuredSelection) view.getSelection()).getFirstElement());
            view.setGroupExpanded(bus, true);
            assertEquals(6, TrackEntry.visibleTracks(view.getStreamList()).size());
            assertEquals("b'1", bus.waveforms.get(0).currentValue);
            assertEquals("b'0", bus.waveforms.get(1).currentValue);
            view.setGroupExpanded(bus, false);
            assertEquals(4, TrackEntry.visibleTracks(view.getStreamList()).size());
            view.deleteSelectedTracks(); drainEvents();
            assertEquals(List.of(inputs.get(0), inputs.get(1), tail), view.getStreamList());
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void combinedVectorsAreTopLevelAfterLastSelectedBitsContainingRoot() throws Exception {
        var inputs = scalarInputs();
        var source = new BitVectorExpansionTest.Signal("original bus", 2);
        source.events.put(0, com.minres.scviewer.database.BitVector.fromString(2, "10"));
        TrackEntry bus = new TrackEntry(source, null);
        bus.setExpanded(true);
        var originalBits = List.copyOf(bus.waveforms);
        TrackEntry inner = TrackEntry.group("inner", List.of(bus, inputs.get(1)), null);
        TrackEntry outer = TrackEntry.group("outer", List.of(inner), null);
        TrackEntry tail = new TrackEntry(new EmptyWaveform("tail"), null);
        view.getStreamList().clear();
        view.getStreamList().addAll(List.of(inputs.get(0), outer, tail));
        view.update();
        WaveformViewer viewer = compositionViewer(display::asyncExec);
        try {
            viewer.combineBits("from bus", List.of(originalBits.get(1), originalBits.get(0)));
            waitUntil(() -> view.getStreamList().size() == 4);
            TrackEntry combined = view.getStreamList().get(2);
            assertEquals("from bus", combined.waveform.getDisplayName());
            assertEquals(List.of(inputs.get(0), outer, combined, tail), view.getStreamList());
            assertEquals(List.of(bus, inputs.get(1)), inner.waveforms);
            assertEquals(originalBits, bus.waveforms);
            viewer.combineBits("mixed", List.of(originalBits.get(0), inputs.get(0)));
            waitUntil(() -> view.getStreamList().size() == 5);
            TrackEntry mixed = view.getStreamList().get(2);
            assertEquals("mixed", mixed.waveform.getDisplayName());
            assertEquals("10", mixed.waveform.getEventsBeforeTime(0)[0].toString());
            assertEquals(List.of(inputs.get(0), outer, mixed, combined, tail), view.getStreamList());
            assertEquals(originalBits, bus.waveforms);
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void closingViewerRejectsACombineResultAlreadyQueuedForPublication() throws Exception {
        var inputs = scalarInputs();
        var pending = new java.util.concurrent.CopyOnWriteArrayList<Runnable>();
        WaveformViewer viewer = compositionViewer(pending::add);
        viewer.combineBits("bus", inputs);
        waitUntil(() -> !pending.isEmpty());
        viewer.closeDatabase();
        pending.forEach(Runnable::run);
        assertEquals(inputs, view.getStreamList());
        assertFalse(viewer.canCombineSelectedBits());
    }

    private List<org.eclipse.swt.widgets.Control> descendants(org.eclipse.swt.widgets.Composite parent) {
        var controls = new ArrayList<org.eclipse.swt.widgets.Control>();
        for (var child : parent.getChildren()) {
            controls.add(child);
            if (child instanceof org.eclipse.swt.widgets.Composite composite) controls.addAll(descendants(composite));
        }
        return controls;
    }

    @Test
    public void combineDialogLabelsDestinationBitsAndAllowsReorderingAndNameValidation() {
        var inputs = scalarInputs();
        class Dialog extends com.minres.scviewer.e4.application.handlers.CombineBitsHandler.CombineDialog {
            Dialog() { super(shell, inputs); }
            void accept() { okPressed(); }
        }
        Dialog dialog = new Dialog();
        dialog.create();
        try {
            var controls = descendants(dialog.getShell());
            var table = (org.eclipse.swt.widgets.Table) controls.stream()
                    .filter(org.eclipse.swt.widgets.Table.class::isInstance).findFirst().orElseThrow();
            assertEquals("1", table.getItem(0).getText(0));
            assertEquals("a", table.getItem(0).getText(1));
            var down = (org.eclipse.swt.widgets.Button) controls.stream()
                    .filter(control -> control instanceof org.eclipse.swt.widgets.Button button
                            && button.getText().equals("Move down")).findFirst().orElseThrow();
            down.notifyListeners(SWT.Selection, new Event());
            assertEquals("b", table.getItem(0).getText(1));
            var name = (org.eclipse.swt.widgets.Text) controls.stream()
                    .filter(org.eclipse.swt.widgets.Text.class::isInstance).findFirst().orElseThrow();
            name.setText(" "); dialog.accept();
            assertNull(dialog.getRequest()); assertFalse(dialog.getShell().isDisposed());
            name.setText("ordered"); dialog.accept();
            assertEquals("ordered", dialog.getRequest().name());
            assertEquals(List.of(inputs.get(1), inputs.get(0)), dialog.getRequest().bits());
        } finally { if (dialog.getShell() != null && !dialog.getShell().isDisposed()) dialog.close(); }
    }

    @Test
    public void bitvectorDisclosureUpdatesAllPanesAndCursorValues() {
        var source = new BitVectorExpansionTest.Signal("bus", 4);
        source.events.put(10, com.minres.scviewer.database.BitVector.fromString(4, "10xz"));
        source.events.put(20, com.minres.scviewer.database.BitVector.fromString(4, "0011"));
        TrackEntry bus = view.addWaveform(source, -1);
        view.setMaxTime(30);
        view.setCursorTime(10);
        view.update();
        assertEquals(TrackEntry.HierState.CLOSED, bus.hierState);
        Event event = new Event();
        event.button = 1;
        event.x = 8;
        event.y = 5;
        view.getNameControl().notifyListeners(SWT.MouseUp, event);
        drainEvents();
        assertEquals(TrackEntry.HierState.OPENED, bus.hierState);
        assertEquals(List.of("b'1", "b'0", "b'x", "b'z"),
                bus.waveforms.stream().map(track -> track.currentValue).toList());
        view.setCursorTime(20);
        view.update();
        assertEquals(List.of("b'0", "b'0", "b'1", "b'1"),
                bus.waveforms.stream().map(track -> track.currentValue).toList());
        view.getNameControl().notifyListeners(SWT.MouseUp, event);
        assertEquals(TrackEntry.HierState.CLOSED, bus.hierState);
        view.getNameControl().notifyListeners(SWT.MouseDoubleClick, event);
        assertEquals(TrackEntry.HierState.OPENED, bus.hierState);
        assertEquals(5, TrackEntry.visibleTracks(view.getStreamList()).size());
    }

    @Test
    public void dragIndicatorMatchesInsertionAndClearsAfterDrop() {
        List<TrackEntry> tracks = tracks(3);
        view.getStreamList().addAll(tracks);
        drainEvents();
        Object location = dropLocation(25);
        invokeView("setStreamDropLocation", new Class<?>[]{location.getClass()}, location);
        assertInsertionLine(20, true);
        moveTracks(List.of(tracks.get(2)), location);
        assertEquals(List.of(tracks.get(0), tracks.get(2), tracks.get(1)), view.getStreamList());
        assertInsertionLine(20, false);
    }

    @Test
    public void bottomInsertionUsesTheEndOfTheLastTrack() {
        List<TrackEntry> tracks = tracks(3);
        view.getStreamList().addAll(tracks);
        drainEvents();
        Object location = dropLocation(100);
        invokeView("setStreamDropLocation", new Class<?>[]{location.getClass()}, location);
        assertInsertionLine(60, true);
        moveTracks(List.of(tracks.get(0)), location);
        assertEquals(List.of(tracks.get(1), tracks.get(2), tracks.get(0)), view.getStreamList());
    }

    @Test
    public void groupedInsertionUsesTheVisibleChildAndRejectsCycles() {
        List<TrackEntry> tracks = tracks(3);
        TrackEntry group = TrackEntry.group("group", List.of(tracks.get(0), tracks.get(1)), null);
        view.getStreamList().addAll(List.of(group, tracks.get(2)));
        drainEvents();
        Object location = dropLocation(45);
        assertEquals(false, invokeView("canDropTracks", new Class<?>[]{List.class, location.getClass()}, List.of(group), location));
        invokeView("setStreamDropLocation", new Class<?>[]{location.getClass()}, location);
        assertInsertionLine(40, true);
        moveTracks(List.of(tracks.get(2)), location);
        assertEquals(List.of(group), view.getStreamList());
        assertEquals(List.of(tracks.get(0), tracks.get(2), tracks.get(1)), group.waveforms);
    }

    private Point origin() {
        try { return (Point) canvas.getClass().getMethod("getOrigin").invoke(canvas); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private void setCanvasOrigin(int y) {
        try { canvas.getClass().getMethod("setOrigin", int.class, int.class).invoke(canvas, origin().x, y); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private void assertAligned() {
        int y = canvas.getVerticalBar().getSelection();
        assertEquals("Canvas transform must agree with scrollbar", -y, origin().y);
        assertEquals("Name pane", y, names.getOrigin().y);
        assertEquals("Value pane", y, values.getOrigin().y);
    }

    private void scrollToBottom() {
        canvas.getVerticalBar().setSelection(canvas.getVerticalBar().getMaximum());
        canvas.getVerticalBar().notifyListeners(SWT.Selection, new Event());
        drainEvents();
        assertTrue(canvas.getVerticalBar().getSelection() > 0);
        assertAligned();
    }

    @Test
    public void pendingTrackUpdatesAreSafeAfterDisposal() {
        view.getStreamList().addAll(tracks(80));
        shell.dispose();
        drainEvents();
    }

    @Test
    public void collapsingLargeGroupResetsAllOriginsWhenContentFits() {
        TrackEntry group = TrackEntry.group("group", tracks(80), null);
        view.getStreamList().add(group);
        view.update(); drainEvents(); scrollToBottom();
        view.setGroupExpanded(group, false);
        assertAligned();
        drainEvents(); assertAligned();
        assertEquals(0, origin().y);
        assertFalse(canvas.getVerticalBar().getEnabled());
        view.setGroupExpanded(group, true);
        drainEvents(); scrollToBottom();
        view.setSelection(new StructuredSelection(group));
        drainEvents(); assertAligned();
        assertEquals("Revealing first track must not create a positive origin", 0, origin().y);
    }

    @Test
    public void deletingTracksAndThenLastTrackKeepsAllOriginsAtZero() {
        List<TrackEntry> tracks = tracks(80);
        view.getStreamList().addAll(tracks);
        view.update(); drainEvents(); scrollToBottom();
        view.setSelection(new StructuredSelection(tracks.subList(1, tracks.size())));
        view.deleteSelectedTracks();
        assertAligned();
        drainEvents(); assertAligned(); assertEquals(0, origin().y);
        view.setSelection(new StructuredSelection(tracks.get(0)));
        view.deleteSelectedTracks();
        assertAligned();
        drainEvents(); assertAligned(); assertEquals(0, origin().y);
        assertTrue(view.getStreamList().isEmpty());
    }

    @Test
    public void allScrollSourcesAndDifferentViewportHeightsStaySynchronized() {
        view.getStreamList().addAll(tracks(80));
        view.update(); drainEvents(); scrollToBottom();
        names.setSize(names.getSize().x, names.getSize().y - 45);
        drainEvents(); assertAligned();
        int maximum = canvas.getVerticalBar().getMaximum() - canvas.getVerticalBar().getThumb();
        assertEquals(maximum, names.getVerticalBar().getMaximum() - names.getVerticalBar().getThumb());
        assertEquals(maximum, values.getVerticalBar().getMaximum() - values.getVerticalBar().getThumb());
        names.getVerticalBar().setSelection(300);
        names.getVerticalBar().notifyListeners(SWT.Selection, new Event());
        drainEvents(); assertAligned(); assertEquals(300, canvas.getVerticalBar().getSelection());
        values.getVerticalBar().setSelection(150);
        values.getVerticalBar().notifyListeners(SWT.Selection, new Event());
        drainEvents(); assertAligned(); assertEquals(150, canvas.getVerticalBar().getSelection());
        // Programmatic changes do not emit a scrollbar Selection event.
        values.setOrigin(values.getOrigin().x, 220);
        drainEvents(); assertAligned(); assertEquals(220, canvas.getVerticalBar().getSelection());
        setCanvasOrigin(20);
        drainEvents(); assertAligned(); assertEquals(0, origin().y);
        shell.setSize(1000, 900);
        shell.layout(true, true);
        drainEvents(); assertAligned();
    }
}
