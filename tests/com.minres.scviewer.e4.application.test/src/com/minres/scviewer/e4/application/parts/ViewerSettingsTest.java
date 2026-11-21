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
import com.minres.scviewer.e4.application.internal.util.ViewerSettings;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry.ValueDisplay;

public class ViewerSettingsTest {
    private Map<String, String> state() {
        return new HashMap<>(Map.of("SHOWN_WAVEFORMS", "1", "SHOWN_WAVEFORM0", "top.stream",
                "SHOWN_WAVEFORM0.WAVEFORM_TYPE", "STREAM", "SHOWN_WAVEFORM0.VALUE_DISPLAY", "BINARY",
                "SHOWN_WAVEFORM0.WAVEFORM_SELECTED", "TRUE", "SHOWN_CURSORS", "1", "SHOWN_CURSOR0", "123"));
    }

    @Test
    public void nestedGroupsRoundTripIncludingHiddenTracksAndDuplicateNames() throws Exception {
        var leaf = new com.minres.scviewer.database.ui.TrackEntry(new EmptyWaveform("leaf"), null);
        leaf.valueDisplay = ValueDisplay.BINARY;
        var inner = com.minres.scviewer.database.ui.TrackEntry.group("same name", List.of(leaf), null);
        var outer = com.minres.scviewer.database.ui.TrackEntry.group("same name", List.of(inner), null);
        outer.hierState = com.minres.scviewer.database.ui.TrackEntry.HierState.CLOSED;
        var empty = com.minres.scviewer.database.ui.TrackEntry.group("empty", List.of(), null);
        var tracks = new java.util.ArrayList<ViewerSettings.Track>();
        var capture = WaveformViewer.class.getDeclaredMethod("captureTracks", List.class, Integer.class, List.class);
        capture.setAccessible(true);
        capture.invoke(null, List.of(outer, empty), null, tracks);
        assertEquals(4, tracks.size());
        assertNull(tracks.get(0).parent());
        assertEquals(Integer.valueOf(0), tracks.get(1).parent());
        assertEquals(Integer.valueOf(1), tracks.get(2).parent());
        assertFalse(tracks.get(0).expanded());
        assertTrue(tracks.get(1).expanded());
        assertTrue(tracks.get(3).group());
        ViewerSettings settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        Path file = Files.createTempFile("group-settings", ".scview");
        try {
            settings.write(file);
            assertEquals(settings, ViewerSettings.read(file));
            Map<String, String> state = new HashMap<>(settings.toMap());
            ViewerSettings legacy = ViewerSettings.parse(state());
            legacy.replaceState(state);
            assertFalse(state.containsKey("SHOWN_WAVEFORM0.GROUP"));
            assertFalse(state.containsKey("SHOWN_WAVEFORM2.PARENT"));
        } finally { Files.deleteIfExists(file); }
    }

    @Test
    public void loadingRebuildsNestedGroupsInsteadOfFlatSeparators() throws Exception {
        var roots = new java.util.ArrayList<com.minres.scviewer.database.ui.TrackEntry>();
        var zoomType = com.minres.scviewer.database.ui.IWaveformZoom.class;
        Object zoom = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{zoomType},
                (proxy, method, args) -> method.getName().equals("timeToString") ? "0" : null);
        IWaveformView pane = (IWaveformView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformView.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "getStreamList": return roots;
                    case "getCursorList": return List.of();
                    case "getWaveformZoom": return zoom;
                    case "getCursorTime": case "getMarkerTime": return 0L;
                    case "getSelectedMarker": return 0;
                    case "addWaveform":
                        var entry = new com.minres.scviewer.database.ui.TrackEntry((com.minres.scviewer.database.IWaveform) args[0], null);
                        roots.add(entry); return entry;
                    default: return null;
                    }
                });
        WaveformViewer viewer = new WaveformViewer();
        var paneField = WaveformViewer.class.getDeclaredField("waveformPane");
        paneField.setAccessible(true); paneField.set(viewer, pane);
        var brokerField = WaveformViewer.class.getDeclaredField("eventBroker");
        brokerField.setAccessible(true);
        brokerField.set(viewer, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{brokerField.getType()},
                (proxy, method, args) -> true));
        var display = com.minres.scviewer.database.ui.TrackEntry.WaveDisplay.DEFAULT;
        var tracks = List.of(
                new ViewerSettings.Track("group", WaveformType.EMPTY, ValueDisplay.DEFAULT, display, false, null, true, false),
                new ViewerSettings.Track("nested", WaveformType.EMPTY, ValueDisplay.DEFAULT, display, false, 0, true, true),
                new ViewerSettings.Track("leaf", WaveformType.EMPTY, ValueDisplay.BINARY, display, false, 1, false, false));
        var settings = new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of());
        var apply = WaveformViewer.class.getDeclaredMethod("applySettings", ViewerSettings.class);
        apply.setAccessible(true);
        try {
            apply.invoke(viewer, ViewerSettings.parse(settings.toMap()));
            assertEquals(1, roots.size());
            assertTrue(roots.get(0).isGroup());
            assertEquals(com.minres.scviewer.database.ui.TrackEntry.HierState.CLOSED, roots.get(0).hierState);
            var nested = roots.get(0).waveforms.get(0);
            assertEquals(com.minres.scviewer.database.ui.TrackEntry.HierState.OPENED, nested.hierState);
            assertEquals("leaf", nested.waveforms.get(0).waveform.getFullName());
            assertEquals(ValueDisplay.BINARY, nested.waveforms.get(0).valueDisplay);
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void rejectsInvalidGroupHierarchyBeforeApplication() {
        for (String parent : List.of("0", "1", "-1", "99999999999", "bad")) {
            Map<String, String> invalid = state();
            invalid.put("SHOWN_WAVEFORM0.PARENT", parent);
            try { ViewerSettings.parse(invalid); fail("Accepted parent " + parent); }
            catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(".PARENT")); }
        }
        Map<String, String> invalid = state();
        invalid.put("SHOWN_WAVEFORM0.GROUP", "true");
        try { ViewerSettings.parse(invalid); fail("Accepted a stream as a group"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("WAVEFORM_TYPE")); }
    }

    @Test
    public void legacyTransactionTypeIsReadAndSavedAsStream() {
        Map<String, String> legacy = state();
        legacy.put("SHOWN_WAVEFORM0.WAVEFORM_TYPE", "TRANSACTION");
        ViewerSettings settings = ViewerSettings.parse(legacy);
        assertEquals(WaveformType.STREAM, settings.tracks().get(0).type());
        assertEquals("STREAM", settings.toMap().get("SHOWN_WAVEFORM0.WAVEFORM_TYPE"));
        assertEquals("TRANSACTION", legacy.get("SHOWN_WAVEFORM0.WAVEFORM_TYPE"));
    }

    @Test
    public void readsExistingFormatIncludingOptionalAndUppercaseValues() {
        ViewerSettings settings = ViewerSettings.parse(state());
        assertEquals("top.stream", settings.tracks().get(0).name());
        assertEquals(WaveformType.STREAM, settings.tracks().get(0).type());
        assertEquals(ValueDisplay.BINARY, settings.tracks().get(0).valueDisplay());
        assertTrue(settings.tracks().get(0).selected());
        assertEquals(Long.valueOf(123), settings.cursors().get(0));
        assertNull(settings.scale());
    }

    @Test
    public void settingsRoundTripWithoutWidgetsAndRemoveObsoletePersistedEntries() throws Exception {
        ViewerSettings original = ViewerSettings.parse(state());
        Path file = Files.createTempFile("settings-roundtrip", ".properties");
        try {
            original.write(file);
            assertEquals(original, ViewerSettings.read(file));
            Map<String, String> persisted = new HashMap<>(Map.of("SHOWN_WAVEFORM99", "stale",
                    "SELECTED_TX_ID", "123", "SELECTED_TRACKENTRY_NAME", "stale", "unrelated", "retained"));
            original.replaceState(persisted);
            assertFalse(persisted.containsKey("SHOWN_WAVEFORM99"));
            assertFalse(persisted.containsKey("SELECTED_TX_ID"));
            assertEquals("retained", persisted.get("unrelated"));
            assertEquals(original, ViewerSettings.parse(persisted));
        } finally { Files.deleteIfExists(file); }
    }

    @Test
    public void rejectsMalformedFieldsBeforeApplication() {
        for (Map.Entry<String, String> entry : Map.of("SHOWN_WAVEFORMS", "-1", "SHOWN_CURSORS", "9999999999",
                "SHOWN_CURSOR0", "time", "SCALING_FACTOR", "0", "BASE_LINE_TIME", "-1",
                "SHOWN_WAVEFORM0.VALUE_DISPLAY", "invalid", "TX_DETAILS_SHOWN", "maybe").entrySet()) {
            Map<String, String> invalid = state();
            invalid.put(entry.getKey(), entry.getValue());
            try { ViewerSettings.parse(invalid); fail("Accepted " + entry.getKey()); }
            catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains(entry.getKey())); }
        }
        Map<String, String> missing = state();
        missing.remove("SHOWN_WAVEFORM0");
        try { ViewerSettings.parse(missing); fail("Accepted missing waveform name"); }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("SHOWN_WAVEFORM0")); }
    }

    @Test
    public void missingUnqualifiedWaveformDoesNotAttemptInvalidSubstring() {
        EmptyWaveform child = new EmptyWaveform("child");
        EmptyWaveform parent = new EmptyWaveform("top") {
            @Override public List<IHierNode> getChildNodes() { return List.of(child); }
        };
        IWaveformDb db = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformDb.class},
                (proxy, method, args) -> method.getName().equals("getStreamByName") && args[0].equals("top") ? parent : null);
        assertNull(ViewerSettings.resolveWaveform(db, "missing"));
        assertNull(ViewerSettings.resolveWaveform(db, "top.absent"));
        assertSame(child, ViewerSettings.resolveWaveform(db, "top.child"));
    }

    @Test
    public void invalidOrUnreadableFileLeavesViewerUntouchedAndReportsError() throws Exception {
        AtomicInteger changes = new AtomicInteger();
        IWaveformView pane = (IWaveformView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformView.class},
                (proxy, method, args) -> { changes.incrementAndGet(); throw new AssertionError("Layout changed before validation"); });
        class Viewer extends WaveformViewer {
            int errors;
            @Override protected void reportSettingsError(String title, String message) { errors++; assertFalse(message.isEmpty()); }
        }
        Viewer viewer = new Viewer();
        Path file = Files.createTempFile("scviewer-settings", ".properties");
        try {
            java.lang.reflect.Field field = WaveformViewer.class.getDeclaredField("waveformPane");
            field.setAccessible(true); field.set(viewer, pane);
            Files.writeString(file, "SHOWN_WAVEFORMS=1\nSHOWN_WAVEFORM0=stream\nSHOWN_WAVEFORM0.WAVE_DISPLAY=unknown\n");
            viewer.loadState(file.toString());
            Files.delete(file);
            viewer.loadState(file.toString());
            assertEquals(2, viewer.errors);
            assertEquals(0, changes.get());
        } finally { viewer.closeDatabase(); Files.deleteIfExists(file); }
    }
}
