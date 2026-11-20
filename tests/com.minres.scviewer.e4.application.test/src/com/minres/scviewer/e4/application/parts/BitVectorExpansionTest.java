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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minres.scviewer.database.BitValue;
import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DoubleVal;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ui.BitWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.HierState;
import com.minres.scviewer.e4.application.internal.util.ViewerSettings;

public class BitVectorExpansionTest {
    static class Signal extends EmptyWaveform {
        final EventList events = new EventList();
        final int width;
        Signal(String name, int width) { super(name); this.width = width; }
        @Override public WaveformType getType() { return WaveformType.SIGNAL; }
        @Override public int getWidth() { return width; }
        @Override public IEventList getEvents() { return events; }
        @Override public IEvent[] getEventsAtTime(long time) { return events.get(time); }
        @Override public IEvent[] getEventsBeforeTime(long time) {
            var entry = events.floorEntry(time);
            return entry == null ? new IEvent[0] : entry.events;
        }
    }

    @Test
    public void expansionKeepsBusAndOrdersBitsFromMostToLeastSignificant() {
        Signal source = new Signal("top.bus", 4);
        source.events.put(10, BitVector.fromString(4, "10xz"));
        TrackEntry bus = new TrackEntry(source, null);
        assertEquals(HierState.CLOSED, bus.hierState);
        assertTrue(bus.waveforms.isEmpty());
        bus.setExpanded(true);
        assertEquals(5, TrackEntry.visibleTracks(List.of(bus)).size());
        assertEquals(List.of("top.bus[3]", "top.bus[2]", "top.bus[1]", "top.bus[0]"),
                bus.waveforms.stream().map(track -> track.waveform.getFullName()).toList());
        assertEquals(List.of("1", "0", "x", "z"), bus.waveforms.stream()
                .map(track -> track.waveform.getEventsBeforeTime(10)[0].toString()).toList());
        TrackEntry bit = bus.waveforms.get(0);
        bit.valueDisplay = TrackEntry.ValueDisplay.BINARY;
        bus.setExpanded(false);
        assertEquals(List.of(bus), TrackEntry.visibleTracks(List.of(bus)));
        bus.setExpanded(true);
        assertSame(bit, bus.waveforms.get(0));
        assertEquals(TrackEntry.ValueDisplay.BINARY, bit.valueDisplay);
        assertSame(bus.waveforms, TrackEntry.siblingsOf(List.of(bus), bit));
        TrackEntry.removeTracks(List.of(bus), List.of(bit));
        assertEquals(3, bus.waveforms.size());
    }

    @Test
    public void projectionsAreReadOnlyViewsWithUnsignedRangesAndNoSourceMutation() {
        Signal source = new Signal("bus", 33);
        BitVector value = new BitVector(33);
        value.setValue(0, BitValue.Z);
        value.setValue(15, BitValue.X);
        value.setValue(16, BitValue.ONE);
        value.setValue(32, BitValue.ONE);
        source.events.put(10, value);
        source.events.put(Long.MIN_VALUE, value);
        source.events.put(-1L, value);
        for (int bit : new int[]{0, 15, 16, 32}) {
            var projected = new BitWaveform(source, bit);
            assertEquals(value.getValue(bit).toChar(), projected.getEventsAtTime(10)[0].toString().charAt(0));
            assertEquals(0, projected.getEventsBeforeTime(9).length);
            assertNull(projected.getEventsAtTime(9));
            var events = projected.getEvents();
            assertEquals(3, events.size());
            assertEquals(Long.MIN_VALUE, events.higherEntry(10).timestamp);
            assertEquals(Long.MIN_VALUE, events.lowerEntry(-1L).timestamp);
            assertEquals(10, events.floorEntry(11).timestamp);
            assertEquals(Long.MIN_VALUE, events.ceilingEntry(11).timestamp);
            assertEquals(2, events.subMap(Long.MIN_VALUE, true, -1L).size());
            assertEquals(1, events.subMap(Long.MIN_VALUE, false, -1L).size());
            assertEquals(3, events.entrySet().size());
            ((BitVector) projected.getEventsAtTime(10)[0]).setValue(0, BitValue.ZERO);
            assertSame(value, source.getEventsAtTime(10)[0]);
            try { events.put(20, value); fail("Projected events must be read-only"); }
            catch (UnsupportedOperationException expected) { }
        }
        assertEquals(BitValue.Z, value.getValue(0));
        assertEquals(BitValue.X, value.getValue(15));
        assertTrue(value.toString().endsWith("z"));
        // An existing projection sees events added later without rebuilding or copying the source.
        var projected = new BitWaveform(source, 16).getEvents();
        source.events.put(20, value);
        assertEquals(4, projected.size());
        assertEquals("1", projected.get(20)[0].toString());
    }

    @Test
    public void realAndScalarSignalsCannotBeExpanded() {
        Signal real = new Signal("real", 64) { @Override public boolean isBitVector() { return false; } };
        real.events.put(0, new DoubleVal(1.5));
        Signal scalar = new Signal("scalar", 1);
        scalar.events.put(0, BitVector.fromString(1, "1"));
        for (Signal source : List.of(real, scalar)) {
            TrackEntry track = new TrackEntry(source, null);
            track.setExpanded(true);
            assertFalse(track.isExpandable());
            assertEquals(HierState.NONE, track.hierState);
            assertTrue(track.waveforms.isEmpty());
        }
    }

    @Test public void vectorMetadataDoesNotReadEventsOnTheUiThread() {
        Signal source = new Signal("lazy", 8) {
            @Override public IEventList getEvents() { throw new AssertionError("Metadata triggered I/O"); }
        };
        TrackEntry track = new TrackEntry(source, null);
        assertTrue(track.isExpandable());
        track.setExpanded(true);
        assertEquals(8, track.waveforms.size());
    }

    @Test
    public void savedExpansionPreservesHiddenBitsAndResolvesProjectedNames() throws Exception {
        Signal source = new Signal("top.bus[3:0]", 4);
        source.events.put(0, BitVector.fromString(4, "1010"));
        TrackEntry bus = new TrackEntry(source, null);
        bus.setExpanded(true);
        bus.waveforms.get(1).selected = true;
        bus.setExpanded(false);
        TrackEntry group = TrackEntry.group("group", List.of(bus), null);
        var tracks = new ArrayList<ViewerSettings.Track>();
        var capture = WaveformViewer.class.getDeclaredMethod("captureTracks", List.class, Integer.class, List.class);
        capture.setAccessible(true);
        capture.invoke(null, List.of(group), null, tracks);
        assertEquals(6, tracks.size());
        assertEquals(Integer.valueOf(0), tracks.get(1).parent());
        assertEquals(Integer.valueOf(1), tracks.get(2).parent());
        assertFalse(tracks.get(1).expanded());
        var settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        assertEquals(settings, ViewerSettings.parse(settings.toMap()));
        IWaveformDb database = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IWaveformDb.class}, (proxy, method, args) ->
                    method.getName().equals("getStreamByName") && source.getFullName().equals(args[0]) ? source : null);
        var bit = ViewerSettings.resolveWaveform(database, tracks.get(2).name());
        assertTrue(bit instanceof BitWaveform);
        assertEquals("1", bit.getEventsBeforeTime(0)[0].toString());
        assertNull(ViewerSettings.resolveWaveform(database, "top.bus[3:0][99]"));
        assertNull(ViewerSettings.resolveWaveform(database, "top.bus[3:0][-1]"));
        assertSame(source, ViewerSettings.resolveWaveform(database, source.getFullName()));
        var restored = restore(settings, database).get(0).waveforms.get(0);
        assertEquals(HierState.CLOSED, restored.hierState);
        assertFalse(restored.isGroup());
        assertEquals(4, restored.waveforms.size());
        restored.setExpanded(true);
        assertEquals(4, restored.waveforms.size());
        assertEquals("1", restored.waveforms.get(0).waveform.getEventsBeforeTime(0)[0].toString());
        bus.setExpanded(true);
        tracks.clear();
        capture.invoke(null, List.of(bus), null, tracks);
        settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        assertTrue(ViewerSettings.parse(settings.toMap()).tracks().get(0).expanded());
        restored = restore(settings, database).get(0);
        assertEquals(HierState.OPENED, restored.hierState);
        assertEquals(5, TrackEntry.visibleTracks(List.of(restored)).size());
    }

    List<TrackEntry> restore(ViewerSettings settings, IWaveformDb database) throws Exception {
        var roots = new ArrayList<TrackEntry>();
        var paneType = com.minres.scviewer.database.ui.IWaveformView.class;
        var zoomType = com.minres.scviewer.database.ui.IWaveformZoom.class;
        Object zoom = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{zoomType},
                (proxy, method, args) -> method.getName().equals("timeToString") ? "0" : null);
        Object pane = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{paneType},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "getStreamList": return roots;
                    case "getCursorList": return List.of();
                    case "getWaveformZoom": return zoom;
                    case "getCursorTime": case "getMarkerTime": return 0L;
                    case "getSelectedMarker": return 0;
                    case "addWaveform":
                        var entry = new TrackEntry((com.minres.scviewer.database.IWaveform) args[0], null);
                        roots.add(entry); return entry;
                    default: return null;
                    }
                });
        WaveformViewer viewer = new WaveformViewer() {
            @Override public IWaveformDb getDatabase() { return database; }
        };
        var paneField = WaveformViewer.class.getDeclaredField("waveformPane");
        paneField.setAccessible(true);
        paneField.set(viewer, pane);
        var brokerField = WaveformViewer.class.getDeclaredField("eventBroker");
        brokerField.setAccessible(true);
        brokerField.set(viewer, Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{brokerField.getType()}, (proxy, method, args) -> true));
        var apply = WaveformViewer.class.getDeclaredMethod("applySettings", ViewerSettings.class);
        apply.setAccessible(true);
        try { apply.invoke(viewer, ViewerSettings.parse(settings.toMap())); }
        finally { viewer.closeDatabase(); }
        return roots;
    }
}
