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
