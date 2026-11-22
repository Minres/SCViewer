/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
import com.minres.scviewer.database.CombinedBitVectorWaveform;
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
        Long transactionId, String transactionTrack, Boolean detailsShown, List<File> files, List<CombinedVector> combinedVectors) {
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

    public record BitSource(String waveform, int bit) {
        public BitSource {
            if (waveform == null || waveform.isBlank() || waveform.startsWith("@combined:") || bit < 0)
                throw new IllegalArgumentException("Invalid recorded bit reference");
        }
    }
    public record CombinedVector(String id, String name, List<BitSource> bits) {
        public CombinedVector {
            if (id == null || !id.startsWith("@combined:") || id.length() <= 10 || name == null || name.isBlank()
                    || bits == null || bits.size() < 2) throw new IllegalArgumentException("Invalid combined vector definition");
            bits = List.copyOf(bits);
        }
    }

    public ViewerSettings(List<Track> tracks, List<Long> cursors, Long scale, Long baseline,
            Long transactionId, String transactionTrack, Boolean detailsShown, List<File> files) {
        this(tracks, cursors, scale, baseline, transactionId, transactionTrack, detailsShown, files, List.of());
    }

    public ViewerSettings {
        tracks = List.copyOf(tracks);
        cursors = List.copyOf(cursors);
        files = List.copyOf(files);
        combinedVectors = List.copyOf(combinedVectors);
        if (combinedVectors.stream().map(CombinedVector::id).distinct().count() != combinedVectors.size())
            throw new IllegalArgumentException("Duplicate combined vector identity");
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
        state.put("COMBINED_VECTORS", Integer.toString(combinedVectors.size()));
        for (int i = 0; i < combinedVectors.size(); i++) {
            CombinedVector vector = combinedVectors.get(i);
            String key = "COMBINED_VECTOR" + i;
            state.put(key + ".ID", vector.id());
            state.put(key + ".NAME", vector.name());
            state.put(key + ".BITS", Integer.toString(vector.bits().size()));
            for (int bit = 0; bit < vector.bits().size(); bit++) {
                state.put(key + ".BIT" + bit + ".SOURCE", vector.bits().get(bit).waveform());
                state.put(key + ".BIT" + bit + ".INDEX", Integer.toString(vector.bits().get(bit).bit()));
            }
        }
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
            if (track.group() || track.type() == WaveformType.SIGNAL || track.type() == WaveformType.STREAM) {
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
        state.keySet().removeIf(key -> key.startsWith("COMBINED_VECTOR") || key.startsWith(DATABASE_FILE) || key.startsWith(SHOWN_WAVEFORM)
                || key.startsWith(SHOWN_CURSOR) || key.equals(SCALING_FACTOR) || key.equals(BASE_LINE_TIME)
                || key.equals(SELECTED_TX_ID) || key.equals(SELECTED_TRACKENTRY_NAME) || key.equals(TX_DETAILS_SHOWN));
        state.putAll(toMap());
    }

    public static ViewerSettings parse(Map<String, String> state) {
        List<CombinedVector> vectors = new ArrayList<>();
        for (int i = 0, n = count(state, "COMBINED_VECTORS"); i < n; i++) {
            String key = "COMBINED_VECTOR" + i;
            List<BitSource> bits = new ArrayList<>();
            for (int j = 0, m = count(state, key + ".BITS"); j < m; j++) {
                String source = key + ".BIT" + j;
                required(state, source + ".INDEX");
                Long bit = number(state, source + ".INDEX");
                if (bit < 0 || bit > Integer.MAX_VALUE) throw invalid(source + ".INDEX", "invalid bit index");
                bits.add(new BitSource(required(state, source + ".SOURCE"), bit.intValue()));
            }
            vectors.add(new CombinedVector(required(state, key + ".ID"), required(state, key + ".NAME"), bits));
        }
        List<Track> tracks = new ArrayList<>();
        int count = count(state, SHOWN_WAVEFORM + "S");
        for (int i = 0; i < count; i++) {
            String key = SHOWN_WAVEFORM + i;
            String name = required(state, key);
            WaveformType type = "TRANSACTION".equals(state.get(key + WAVEFORM_TYPE)) ? WaveformType.STREAM
                    : enumeration(state, key + WAVEFORM_TYPE, WaveformType.class, null);
            Long parent = number(state, key + ".PARENT");
            if (parent != null && (parent < 0 || parent >= i || !(tracks.get(parent.intValue()).group()
                    || tracks.get(parent.intValue()).type() == WaveformType.SIGNAL
                    || tracks.get(parent.intValue()).type() == WaveformType.STREAM)))
                throw invalid(key + ".PARENT", "must reference an earlier group, signal, or stream");
            if (parent != null && tracks.get(parent.intValue()).type() == WaveformType.STREAM
                    && type != WaveformType.GENERATOR)
                throw invalid(key + ".PARENT", "stream children must be generators");
            boolean group = Boolean.TRUE.equals(bool(state, key + ".GROUP"));
            Boolean expanded = bool(state, key + ".EXPANDED");
            if (group && type != WaveformType.EMPTY) throw invalid(key + WAVEFORM_TYPE, "group must have EMPTY type");
            if (!group && expanded != null && type != WaveformType.SIGNAL && type != WaveformType.STREAM)
                throw invalid(key + ".EXPANDED", "requires a group, signal, or stream");
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
                tx, track, bool(state, TX_DETAILS_SHOWN), List.copyOf(databaseFiles(state)), vectors);
    }

    public static List<File> databaseFiles(Map<String, String> state) {
        List<File> files = new ArrayList<>();
        for (int i = 0, n = count(state, DATABASE_FILE + "S"); i < n; i++)
            files.add(new File(required(state, DATABASE_FILE + i)));
        return files;
    }

    /** Definitions only reference recorded waveforms, so reconstruction cannot create cycles. */
    public Map<String, IWaveform> resolveCombinedVectors(IWaveformDb database, List<String> missing) {
        Map<String, IWaveform> resolved = new HashMap<>();
        for (CombinedVector vector : combinedVectors) {
            List<CombinedBitVectorWaveform.SourceBit> bits = new ArrayList<>();
            for (BitSource bit : vector.bits()) {
                IWaveform waveform = resolveWaveform(database, bit.waveform());
                if (waveform == null || waveform.getType() != WaveformType.SIGNAL || bit.bit() >= waveform.getWidth()) {
                    missing.add(vector.name() + ": " + bit.waveform() + "[" + bit.bit() + "]");
                    break;
                }
                if (!waveform.isBitVector()) {
                    missing.add(vector.name() + ": " + bit.waveform() + " is not a bit signal");
                    break;
                }
                bits.add(new CombinedBitVectorWaveform.SourceBit(waveform, bit.bit()));
            }
            if (bits.size() == vector.bits().size())
                resolved.put(vector.id(), new CombinedBitVectorWaveform(vector.id(), vector.name(), bits));
        }
        return resolved;
    }

    public String displayName(String identity) {
        for (CombinedVector vector : combinedVectors) {
            if (identity.equals(vector.id())) return vector.name();
            if (identity.startsWith(vector.id() + "[")) return vector.name() + identity.substring(vector.id().length());
        }
        return identity;
    }

    public static IWaveform resolveWaveform(IWaveformDb database, String name) {
        return resolveWaveform(database, name, Map.of());
    }

    public static IWaveform resolveWaveform(IWaveformDb database, String name, Map<String, IWaveform> derived) {
        IWaveform waveform = derived.get(name);
        if (waveform == null && database != null) waveform = database.getStreamByName(name);
        if (waveform != null) return waveform;
        int bitStart = name.lastIndexOf('[');
        if (bitStart > 0 && name.endsWith("]")) {
            String sourceName = name.substring(0, bitStart);
            IWaveform source = derived.get(sourceName);
            if (source == null && database != null) source = database.getStreamByName(sourceName);
            if (source != null && source.getType() == WaveformType.SIGNAL) {
                try {
                    int bit = Integer.parseInt(name.substring(bitStart + 1, name.length() - 1));
                    if (bit >= 0 && bit < source.getWidth()) return new BitWaveform(source, bit);
                } catch (NumberFormatException e) { /* A declared signal range is not a projected bit. */ }
            }
        }
        if (database == null) return null;
        for (int separator = name.lastIndexOf('.'); separator > 0; separator = name.lastIndexOf('.', separator - 1)) {
            IWaveform parent = database.getStreamByName(name.substring(0, separator));
            if (parent == null) continue;
            String childName = name.substring(separator + 1);
            IWaveform child = parent.getChildNodes().stream().filter(IWaveform.class::isInstance).map(IWaveform.class::cast)
                    .filter(candidate -> childName.equals(candidate.getName())).findFirst().orElse(null);
            if (child != null) return child;
        }
        return null;
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
