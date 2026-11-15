package com.minres.scviewer.e4.application.parts;

import static com.minres.scviewer.e4.application.parts.WaveformViewer.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.ui.TrackEntry.ValueDisplay;
import com.minres.scviewer.database.ui.TrackEntry.WaveDisplay;

/** Validates the existing properties format before any viewer state is changed. */
record ViewerSettings(List<Track> tracks, List<Long> cursors, Long scale, Long baseline,
        Long transactionId, String transactionTrack, Boolean detailsShown) {
    record Track(String name, WaveformType type, ValueDisplay valueDisplay, WaveDisplay waveDisplay, boolean selected) {}

    static ViewerSettings parse(Map<String, String> state) {
        List<Track> tracks = new ArrayList<>();
        int count = count(state, SHOWN_WAVEFORM + "S");
        for (int i = 0; i < count; i++) {
            String key = SHOWN_WAVEFORM + i;
            String name = required(state, key);
            WaveformType type = enumeration(state, key + WAVEFORM_TYPE, WaveformType.class, null);
            tracks.add(new Track(name, type,
                    enumeration(state, key + VALUE_DISPLAY, ValueDisplay.class, ValueDisplay.DEFAULT),
                    enumeration(state, key + WAVE_DISPLAY, WaveDisplay.class, WaveDisplay.DEFAULT),
                    Boolean.TRUE.equals(bool(state, key + WAVEFORM_SELECTED))));
        }
        List<Long> cursors = new ArrayList<>();
        for (int i = 0, n = count(state, SHOWN_CURSOR + "S"); i < n; i++) {
            required(state, SHOWN_CURSOR + i);
            cursors.add(number(state, SHOWN_CURSOR + i));
        }
        Long scale = number(state, SCALING_FACTOR);
        if (scale != null && scale <= 0) throw invalid(SCALING_FACTOR, "must be positive");
        Long baseline = number(state, BASE_LINE_TIME);
        if (baseline != null && baseline < 0) throw invalid(BASE_LINE_TIME, "must not be negative");
        Long tx = number(state, SELECTED_TX_ID);
        String track = state.get(SELECTED_TRACKENTRY_NAME);
        if (tx != null) required(state, SELECTED_TRACKENTRY_NAME);
        return new ViewerSettings(List.copyOf(tracks), List.copyOf(cursors), scale, baseline,
                tx, track, bool(state, TX_DETAILS_SHOWN));
    }

    static List<File> databaseFiles(Map<String, String> state) {
        List<File> files = new ArrayList<>();
        for (int i = 0, n = count(state, DATABASE_FILE + "S"); i < n; i++)
            files.add(new File(required(state, DATABASE_FILE + i)));
        return files;
    }

    static IWaveform resolveWaveform(IWaveformDb database, String name) {
        IWaveform waveform = database.getStreamByName(name);
        if (waveform != null) return waveform;
        int separator = name.lastIndexOf('.');
        if (separator <= 0 || separator == name.length() - 1) return null;
        IWaveform parent = database.getStreamByName(name.substring(0, separator));
        if (parent == null) return null;
        return parent.getChildNodes().stream().filter(IWaveform.class::isInstance).map(IWaveform.class::cast)
                .filter(child -> name.substring(separator + 1).equals(child.getName())).findFirst().orElse(null);
    }

    static ITx findTransaction(IWaveform waveform, long id) {
        for (EventEntry entry : waveform.getEvents().entrySet()) {
            if (entry.events == null) continue;
            for (IEvent event : entry.events)
                if (event instanceof ITxEvent txEvent && txEvent.getTransaction().getId() == id)
                    return txEvent.getTransaction();
        }
        return null;
    }

    private static int count(Map<String, String> state, String key) {
        Long value = number(state, key);
        if (value == null) return 0;
        if (value < 0 || value > state.size()) throw invalid(key, "count exceeds the available entries");
        return value.intValue();
    }

    private static String required(Map<String, String> state, String key) {
        String value = state.get(key);
        if (value == null) throw invalid(key, "is missing");
        return value;
    }

    private static Long number(Map<String, String> state, String key) {
        if (!state.containsKey(key)) return null;
        try { return Long.valueOf(state.get(key)); }
        catch (NumberFormatException e) { throw invalid(key, "must be an integer"); }
    }

    private static Boolean bool(Map<String, String> state, String key) {
        if (!state.containsKey(key)) return null;
        String value = state.get(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw invalid(key, "must be true or false");
    }

    private static <E extends Enum<E>> E enumeration(Map<String, String> state, String key, Class<E> type, E fallback) {
        if (!state.containsKey(key)) return fallback;
        try { return Enum.valueOf(type, required(state, key)); }
        catch (IllegalArgumentException e) { throw invalid(key, "has an unknown value: " + state.get(key)); }
    }

    private static IllegalArgumentException invalid(String key, String reason) {
        return new IllegalArgumentException(key + " " + reason);
    }
}
