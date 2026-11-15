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
