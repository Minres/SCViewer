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
package com.minres.scviewer.e4.application.internal.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Properties;
import java.util.HashMap;
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
import com.minres.scviewer.database.ui.BitWaveform;

/** Validates the existing properties format before any viewer state is changed. */
public record ViewerSettings(List<Track> tracks, List<Long> cursors, Long scale, Long baseline,
        Long transactionId, String transactionTrack, Boolean detailsShown, List<File> files) {
    public static final String DATABASE_FILE = "DATABASE_FILE";
    public static final String SHOWN_WAVEFORM = "SHOWN_WAVEFORM";
    public static final String WAVEFORM_TYPE = ".WAVEFORM_TYPE";
    public static final String VALUE_DISPLAY = ".VALUE_DISPLAY";
    public static final String WAVE_DISPLAY = ".WAVE_DISPLAY";
    public static final String SHOWN_CURSOR = "SHOWN_CURSOR";
    public static final String SCALING_FACTOR = "SCALING_FACTOR";
    public static final String BASE_LINE_TIME = "BASE_LINE_TIME";
    public static final String SELECTED_TX_ID = "SELECTED_TX_ID";
    public static final String SELECTED_TRACKENTRY_NAME = "SELECTED_TRACKENTRY_NAME";
    public static final String WAVEFORM_SELECTED = ".WAVEFORM_SELECTED";
    public static final String TX_DETAILS_SHOWN = "TX_DETAILS_SHOWN";

    /** Tracks are stored in preorder, including hidden descendants. PARENT is an index
     * into this list; it must refer to an earlier group or signal, making cycles impossible.
     * Legacy entries have no parent or group flags and remain ordinary root tracks.
     */
    public record Track(String name, WaveformType type, ValueDisplay valueDisplay, WaveDisplay waveDisplay,
            boolean selected, Integer parent, boolean group, boolean expanded) {
        public Track(String name, WaveformType type, ValueDisplay valueDisplay, WaveDisplay waveDisplay, boolean selected) {
            this(name, type, valueDisplay, waveDisplay, selected, null, false, false);
        }
    }

    public ViewerSettings {
        tracks = List.copyOf(tracks);
        cursors = List.copyOf(cursors);
        files = List.copyOf(files);
    }

    public static ViewerSettings read(Path path) throws IOException {
        Properties properties = new Properties();
        try (java.io.InputStream input = Files.newInputStream(path)) { properties.load(input); }
        Map<String, String> state = new HashMap<>();
        properties.stringPropertyNames().forEach(key -> state.put(key, properties.getProperty(key)));
        return parse(state);
    }

    public void write(Path path) throws IOException {
        Properties properties = new Properties();
        properties.putAll(toMap());
        try (java.io.OutputStream output = Files.newOutputStream(path)) { properties.store(output, "Written by SCViewer"); }
    }

    public Map<String, String> toMap() {
        Map<String, String> state = new HashMap<>();
        state.put(DATABASE_FILE + "S", Integer.toString(files.size()));
        for (int i = 0; i < files.size(); i++) state.put(DATABASE_FILE + i, files.get(i).getAbsolutePath());
        state.put(SHOWN_WAVEFORM + "S", Integer.toString(tracks.size()));
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            String key = SHOWN_WAVEFORM + i;
            state.put(key, track.name());
            if (track.parent() != null) state.put(key + ".PARENT", track.parent().toString());
            if (track.group()) {
                state.put(key + ".GROUP", "true");
            }
            if (track.group() || track.type() == WaveformType.SIGNAL) {
                state.put(key + ".EXPANDED", Boolean.toString(track.expanded()));
            }
            if (track.type() != null) state.put(key + WAVEFORM_TYPE, track.type().name());
            state.put(key + WAVEFORM_SELECTED, Boolean.toString(track.selected()));
            state.put(key + VALUE_DISPLAY, track.valueDisplay().name());
            state.put(key + WAVE_DISPLAY, track.waveDisplay().name());
        }
        state.put(SHOWN_CURSOR + "S", Integer.toString(cursors.size()));
        for (int i = 0; i < cursors.size(); i++) state.put(SHOWN_CURSOR + i, cursors.get(i).toString());
        if (scale != null) state.put(SCALING_FACTOR, scale.toString());
        if (baseline != null) state.put(BASE_LINE_TIME, baseline.toString());
        if (transactionId != null && transactionTrack != null) {
            state.put(SELECTED_TX_ID, transactionId.toString());
            state.put(SELECTED_TRACKENTRY_NAME, transactionTrack);
        }
        if (detailsShown != null) state.put(TX_DETAILS_SHOWN, detailsShown.toString());
        return state;
    }

    public void replaceState(Map<String, String> state) {
        state.keySet().removeIf(key -> key.startsWith(DATABASE_FILE) || key.startsWith(SHOWN_WAVEFORM)
                || key.startsWith(SHOWN_CURSOR) || key.equals(SCALING_FACTOR) || key.equals(BASE_LINE_TIME)
                || key.equals(SELECTED_TX_ID) || key.equals(SELECTED_TRACKENTRY_NAME) || key.equals(TX_DETAILS_SHOWN));
        state.putAll(toMap());
    }

    public static ViewerSettings parse(Map<String, String> state) {
        List<Track> tracks = new ArrayList<>();
        int count = count(state, SHOWN_WAVEFORM + "S");
        for (int i = 0; i < count; i++) {
            String key = SHOWN_WAVEFORM + i;
            String name = required(state, key);
            WaveformType type = "TRANSACTION".equals(state.get(key + WAVEFORM_TYPE)) ? WaveformType.STREAM
                    : enumeration(state, key + WAVEFORM_TYPE, WaveformType.class, null);
            Long parent = number(state, key + ".PARENT");
            if (parent != null && (parent < 0 || parent >= i || !(tracks.get(parent.intValue()).group()
                    || tracks.get(parent.intValue()).type() == WaveformType.SIGNAL)))
                throw invalid(key + ".PARENT", "must reference an earlier group or signal");
            boolean group = Boolean.TRUE.equals(bool(state, key + ".GROUP"));
            Boolean expanded = bool(state, key + ".EXPANDED");
            if (group && type != WaveformType.EMPTY) throw invalid(key + WAVEFORM_TYPE, "group must have EMPTY type");
            if (!group && expanded != null && type != WaveformType.SIGNAL)
                throw invalid(key + ".EXPANDED", "requires a group or signal");
            tracks.add(new Track(name, type,
                    enumeration(state, key + VALUE_DISPLAY, ValueDisplay.class, ValueDisplay.DEFAULT),
                    enumeration(state, key + WAVE_DISPLAY, WaveDisplay.class, WaveDisplay.DEFAULT),
                    Boolean.TRUE.equals(bool(state, key + WAVEFORM_SELECTED)),
                    parent == null ? null : parent.intValue(), group,
                    group ? !Boolean.FALSE.equals(expanded) : Boolean.TRUE.equals(expanded)));
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
                tx, track, bool(state, TX_DETAILS_SHOWN), List.copyOf(databaseFiles(state)));
    }

    public static List<File> databaseFiles(Map<String, String> state) {
        List<File> files = new ArrayList<>();
        for (int i = 0, n = count(state, DATABASE_FILE + "S"); i < n; i++)
            files.add(new File(required(state, DATABASE_FILE + i)));
        return files;
    }

    public static IWaveform resolveWaveform(IWaveformDb database, String name) {
        IWaveform waveform = database.getStreamByName(name);
        if (waveform != null) return waveform;
        int bitStart = name.lastIndexOf('[');
        if (bitStart > 0 && name.endsWith("]")) {
            IWaveform source = database.getStreamByName(name.substring(0, bitStart));
            if (source != null && source.getType() == WaveformType.SIGNAL) {
                try {
                    int bit = Integer.parseInt(name.substring(bitStart + 1, name.length() - 1));
                    if (bit >= 0 && bit < source.getWidth()) return new BitWaveform(source, bit);
                } catch (NumberFormatException e) { /* A declared signal range is not a projected bit. */ }
            }
        }
        int separator = name.lastIndexOf('.');
        if (separator <= 0 || separator == name.length() - 1) return null;
        IWaveform parent = database.getStreamByName(name.substring(0, separator));
        if (parent == null) return null;
        return parent.getChildNodes().stream().filter(IWaveform.class::isInstance).map(IWaveform.class::cast)
                .filter(child -> name.substring(separator + 1).equals(child.getName())).findFirst().orElse(null);
    }

    public static ITx findTransaction(IWaveform waveform, long id) {
        if (waveform instanceof com.minres.scviewer.database.tx.ITransactionProvider indexed)
            return indexed.getTransaction(id);
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
