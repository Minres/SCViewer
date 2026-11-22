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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.HierState;
import com.minres.scviewer.e4.application.internal.util.ViewerSettings;

public class GeneratorExpansionTest {
    static class Waveform extends EmptyWaveform {
        final WaveformType type;
        final List<IHierNode> children = new ArrayList<>();
        final EventList events = new EventList();
        IHierNode parent;
        Waveform(String name, WaveformType type) { super(name); this.type = type; }
        @Override public WaveformType getType() { return type; }
        @Override public List<IHierNode> getChildNodes() { return children; }
        @Override public IHierNode getParent() { return parent; }
        @Override public String getFullName() {
            return parent == null ? getName() : parent.getFullName() + "." + getName();
        }
        @Override public int getRowCount() { return 1; }
        @Override public IEventList getEvents() { return events; }
        Waveform generator(String name) {
            Waveform child = new Waveform(name, WaveformType.GENERATOR);
            child.parent = this;
            children.add(child);
            return child;
        }
    }

    static Waveform stream() {
        Waveform stream = new Waveform("top.stream", WaveformType.STREAM);
        stream.generator("write");
        stream.generator("transfer.read");
        return stream;
    }

    @Test public void expansionRetainsTheStreamAndCreatesOnlyItsGeneratorsInMetadataOrder() {
        Waveform stream = stream();
        stream.children.add(1, new HierNode("metadata"));
        stream.children.add(new Waveform("signal", WaveformType.SIGNAL));
        TrackEntry track = new TrackEntry(stream, null);
        assertEquals(HierState.CLOSED, track.hierState);
        assertTrue(track.isExpandable());
        assertTrue(track.waveforms.isEmpty());
        track.setExpanded(true);
        assertEquals(List.of("top.stream.write", "top.stream.transfer.read"), track.waveforms.stream()
                .map(child -> child.waveform.getFullName()).toList());
        assertEquals(3, TrackEntry.visibleTracks(List.of(track)).size());
        assertSame(stream.children.get(0), track.waveforms.get(0).waveform);
        TrackEntry first = track.waveforms.get(0);
        first.valueDisplay = TrackEntry.ValueDisplay.BINARY;
        track.setExpanded(false);
        assertEquals(List.of(track), TrackEntry.visibleTracks(List.of(track)));
        track.setExpanded(true);
        assertSame(first, track.waveforms.get(0));
        assertEquals(TrackEntry.ValueDisplay.BINARY, first.valueDisplay);
        assertSame(track.waveforms, TrackEntry.siblingsOf(List.of(track), first));
        assertEquals(4, stream.children.size());
    }

    @Test public void streamsWithoutGeneratorsAndGeneratorTracksHaveNoDisclosureArrow() {
        Waveform stream = new Waveform("empty", WaveformType.STREAM);
        stream.children.add(new HierNode("metadata"));
        Waveform generator = new Waveform("generator", WaveformType.GENERATOR);
        generator.generator("nested");
        for (Waveform source : List.of(stream, generator)) {
            TrackEntry track = new TrackEntry(source, null);
            track.setExpanded(true);
            assertFalse(track.isExpandable());
            assertEquals(HierState.NONE, track.hierState);
            assertTrue(track.waveforms.isEmpty());
        }
    }

    @Test public void expandingStreamsDoesNotLoadTransactionEvents() {
        Waveform stream = new Waveform("lazy", WaveformType.STREAM) {
            @Override public IEventList getEvents() { throw new AssertionError("Expansion triggered event I/O"); }
        };
        stream.children.add(new Waveform("generator", WaveformType.GENERATOR) {
            @Override public IEventList getEvents() { throw new AssertionError("Expansion loaded a generator"); }
        });
        TrackEntry track = new TrackEntry(stream, null);
        track.setExpanded(true);
        assertEquals(1, track.waveforms.size());
        track.setExpanded(false);
        track.setExpanded(true);
        assertEquals(1, track.waveforms.size());
    }

    @Test public void generatorLayoutRoundTripsWithCollapsedParentsAndDottedGeneratorNames() throws Exception {
        Waveform stream = stream();
        TrackEntry track = new TrackEntry(stream, null);
        track.setExpanded(true);
        track.waveforms.remove(0);
        track.waveforms.get(0).waveDisplay = TrackEntry.WaveDisplay.STEP_WISE;
        track.setExpanded(false);
        TrackEntry group = TrackEntry.group("transactions", List.of(track), null);
        var tracks = new ArrayList<ViewerSettings.Track>();
        var capture = WaveformViewer.class.getDeclaredMethod("captureTracks", List.class, Integer.class, List.class);
        capture.setAccessible(true);
        capture.invoke(null, List.of(group), null, tracks);
        assertEquals(3, tracks.size());
        assertEquals(Integer.valueOf(1), tracks.get(2).parent());
        assertFalse(tracks.get(1).expanded());
        var settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        assertEquals(settings, ViewerSettings.parse(settings.toMap()));
        IWaveformDb database = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IWaveformDb.class}, (proxy, method, args) ->
                    method.getName().equals("getStreamByName") && stream.getFullName().equals(args[0]) ? stream : null);
        assertSame(stream.children.get(1), ViewerSettings.resolveWaveform(database, "top.stream.transfer.read"));
        TrackEntry restored = new BitVectorExpansionTest().restore(settings, database).get(0).waveforms.get(0);
        assertEquals(HierState.CLOSED, restored.hierState);
        assertFalse(restored.isGroup());
        restored.setExpanded(true);
        assertEquals(1, restored.waveforms.size());
        assertSame(stream.children.get(1), restored.waveforms.get(0).waveform);
        assertEquals(TrackEntry.WaveDisplay.STEP_WISE, restored.waveforms.get(0).waveDisplay);
        tracks.clear();
        capture.invoke(null, List.of(restored), null, tracks);
        settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        restored = new BitVectorExpansionTest().restore(settings, database).get(0);
        assertEquals(HierState.OPENED, restored.hierState);
        assertEquals(2, TrackEntry.visibleTracks(List.of(restored)).size());
    }

    @Test public void independentGeneratorBeforeItsParentRestoresAsADistinctDisplayCopy() throws Exception {
        Waveform stream = stream();
        TrackEntry parent = new TrackEntry(stream, null);
        parent.setExpanded(true);
        TrackEntry copy = new TrackEntry(parent.waveforms.get(0).waveform, null);
        copy.valueDisplay = TrackEntry.ValueDisplay.BINARY;
        var tracks = new ArrayList<ViewerSettings.Track>();
        var capture = WaveformViewer.class.getDeclaredMethod("captureTracks", List.class, Integer.class, List.class);
        capture.setAccessible(true);
        capture.invoke(null, List.of(copy, parent), null, tracks);
        var settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        IWaveformDb database = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IWaveformDb.class}, (proxy, method, args) ->
                    method.getName().equals("getStreamByName") && stream.getFullName().equals(args[0]) ? stream : null);
        var restored = new BitVectorExpansionTest().restore(settings, database);
        assertEquals(2, restored.size());
        assertEquals(2, restored.get(1).waveforms.size());
        TrackEntry original = restored.get(1).waveforms.get(0);
        assertNotSame(restored.get(0), original);
        assertSame(restored.get(0).waveform, original.waveform);
        assertEquals(TrackEntry.ValueDisplay.BINARY, restored.get(0).valueDisplay);
        assertEquals(TrackEntry.ValueDisplay.DEFAULT, original.valueDisplay);
    }

    @Test public void settingsRejectChildrenUnderGeneratorsAndNonGeneratorChildrenUnderStreams() {
        for (WaveformType parent : List.of(WaveformType.STREAM, WaveformType.GENERATOR)) {
            var settings = new ViewerSettings(List.of(
                    new ViewerSettings.Track("parent", parent, TrackEntry.ValueDisplay.DEFAULT, TrackEntry.WaveDisplay.DEFAULT,
                            false, null, false, false),
                    new ViewerSettings.Track("child", WaveformType.SIGNAL, TrackEntry.ValueDisplay.DEFAULT,
                            TrackEntry.WaveDisplay.DEFAULT, false, 0, false, false)),
                    List.of(), null, null, null, null, null, List.of());
            try { ViewerSettings.parse(settings.toMap()); fail("Accepted an invalid stream hierarchy"); }
            catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(".PARENT")); }
        }
    }
}
