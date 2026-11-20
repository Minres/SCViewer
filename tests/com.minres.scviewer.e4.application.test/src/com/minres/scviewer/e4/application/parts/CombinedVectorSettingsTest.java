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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.minres.scviewer.database.*;
import com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit;
import com.minres.scviewer.database.ui.*;
import com.minres.scviewer.e4.application.internal.util.ViewerSettings;

public class CombinedVectorSettingsTest {
    private final BitVectorExpansionTest.Signal input = new BitVectorExpansionTest.Signal("top.input", 4);
    private IWaveformDb database() {
        return (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformDb.class},
                (proxy, method, args) -> method.getName().equals("getStreamByName")
                        && input.getFullName().equals(args[0]) ? input : null);
    }
    private CombinedBitVectorWaveform vector() {
        input.events.put(0, BitVector.fromString(4, "10xz"));
        return new CombinedBitVectorWaveform("same label", List.of(new SourceBit(input, 0), new SourceBit(input, 3)));
    }
    private ViewerSettings capture(List<TrackEntry> roots) throws Exception {
        var tracks = new ArrayList<ViewerSettings.Track>();
        Map<String, ViewerSettings.CombinedVector> definitions = new LinkedHashMap<>();
        var capture = WaveformViewer.class.getDeclaredMethod("captureTracks", List.class, Integer.class, List.class);
        capture.setAccessible(true); capture.invoke(null, roots, null, tracks);
        var combined = WaveformViewer.class.getDeclaredMethod("captureCombined", List.class, Map.class);
        combined.setAccessible(true); combined.invoke(null, roots, definitions);
        return new ViewerSettings(tracks, List.of(), null, null, null, null, null, List.of(), List.copyOf(definitions.values()));
    }

    @Test public void eligibilityAcceptsExpandedBitsAndRejectsMixedOrUnrelatedSelections() {
        var bus = new TrackEntry(vector(), null); bus.setExpanded(true);
        var roots = List.of(TrackEntry.group("group", List.of(bus), null));
        var first = bus.waveforms.get(0); var second = bus.waveforms.get(1);
        assertEquals(List.of(first, second), BitVectorComposition.selectedBits(roots, List.of(second, first)));
        assertTrue(BitVectorComposition.selectedBits(roots, List.of(bus, first)).isEmpty());
        assertTrue(BitVectorComposition.selectedBits(roots, List.of(first)).isEmpty());
        assertTrue(BitVectorComposition.selectedBits(roots, List.of(first, new TrackEntry(input, null))).isEmpty());
        var real = new BitVectorExpansionTest.Signal("real", 1);
        real.events.put(0, new DoubleVal(1.0));
        assertFalse(BitVectorComposition.isSingleBit(new TrackEntry(real, null)));
        var sources = BitVectorComposition.sources(List.of(second, first));
        var combined = new CombinedBitVectorWaveform("reversed", sources);
        assertEquals("1z", combined.getEventsBeforeTime(0)[0].toString());
        assertSame(input, combined.getSources().get(0).waveform());
    }

    @Test public void expandedVectorsRoundTripWithIdentitySeparateFromLabelsAndBitOrdering() throws Exception {
        var vector = vector();
        var bus = new TrackEntry(vector, null); bus.setExpanded(true);
        var other = new TrackEntry(new CombinedBitVectorWaveform(vector.getName(), vector.getSources()), null);
        var settings = capture(List.of(TrackEntry.group("group", List.of(bus, other), null)));
        var parsed = ViewerSettings.parse(settings.toMap());
        assertEquals(settings, parsed);
        assertEquals(2, parsed.combinedVectors().size());
        assertEquals("same label", parsed.combinedVectors().get(0).name());
        assertNotEquals(parsed.combinedVectors().get(0).id(), parsed.combinedVectors().get(1).id());
        var roots = new BitVectorExpansionTest().restore(parsed, database());
        var restored = roots.get(0).waveforms.get(0);
        assertEquals(TrackEntry.HierState.OPENED, restored.hierState);
        assertEquals(2, restored.waveforms.size());
        assertEquals("same label", restored.waveform.getDisplayName());
        assertEquals("z1", restored.waveform.getEventsBeforeTime(0)[0].toString());
        assertEquals("same label[1]", restored.waveforms.get(0).waveform.getDisplayName());
        assertEquals("z", restored.waveforms.get(0).waveform.getEventsBeforeTime(0)[0].toString());
        assertEquals(2, roots.get(0).waveforms.size());
    }

    @Test public void survivingBitRetainsDefinitionWhenOriginalBusWasDeleted() throws Exception {
        var bit = new TrackEntry(new BitWaveform(vector(), 1), null);
        var settings = capture(List.of(bit));
        assertEquals(1, settings.combinedVectors().size());
        var restored = new BitVectorExpansionTest().restore(settings, database());
        assertEquals(1, restored.size());
        assertEquals("z", restored.get(0).waveform.getEventsBeforeTime(0)[0].toString());
    }

    @Test public void missingInputsSkipWholeBusAndLeaveOtherTracksIntact() throws Exception {
        var bus = new TrackEntry(vector(), null); bus.setExpanded(true);
        var settings = capture(List.of(bus, new TrackEntry(input, null)));
        var map = new HashMap<>(settings.toMap());
        map.put("COMBINED_VECTOR0.BIT0.SOURCE", "missing.source");
        settings = ViewerSettings.parse(map);
        List<String> missing = new ArrayList<>();
        assertTrue(settings.resolveCombinedVectors(database(), missing).isEmpty());
        assertTrue(missing.get(0).contains("missing.source"));
        var roots = new BitVectorExpansionTest().restore(settings, database());
        assertEquals(1, roots.size()); assertSame(input, roots.get(0).waveform);
        map.put("COMBINED_VECTOR0.BIT0.SOURCE", input.getFullName());
        map.put("COMBINED_VECTOR0.BIT0.INDEX", "99");
        assertTrue(ViewerSettings.parse(map).resolveCombinedVectors(database(), new ArrayList<>()).isEmpty());
    }

    @Test public void validatesDefinitionsAndRemovesObsoleteDefinitionsWhenReplacingState() throws Exception {
        var settings = capture(List.of(new TrackEntry(vector(), null)));
        for (Map.Entry<String, String> invalid : Map.of(
                "COMBINED_VECTOR0.BIT0.INDEX", "-1",
                "COMBINED_VECTOR0.BIT1.INDEX", "99999999999",
                "COMBINED_VECTOR0.BIT0.SOURCE", settings.combinedVectors().get(0).id(),
                "COMBINED_VECTOR0.BITS", "1",
                "COMBINED_VECTOR0.NAME", " ").entrySet()) {
            var map = new HashMap<>(settings.toMap()); map.put(invalid.getKey(), invalid.getValue());
            try { ViewerSettings.parse(map); fail("Accepted " + invalid); } catch (IllegalArgumentException expected) { }
        }
        var map = new HashMap<>(settings.toMap());
        ViewerSettings legacy = ViewerSettings.parse(Map.of());
        legacy.replaceState(map);
        assertEquals(legacy, ViewerSettings.parse(map));
        assertFalse(map.containsKey("COMBINED_VECTOR0.ID"));
        assertTrue(ViewerSettings.parse(Map.of()).combinedVectors().isEmpty());
        var duplicate = new HashMap<>(settings.toMap());
        duplicate.put("COMBINED_VECTORS", "2");
        settings.toMap().forEach((key, value) -> {
            if (key.startsWith("COMBINED_VECTOR0."))
                duplicate.put(key.replace("COMBINED_VECTOR0.", "COMBINED_VECTOR1."), value);
        });
        try { ViewerSettings.parse(duplicate); fail("Accepted duplicate identities"); }
        catch (IllegalArgumentException expected) { }
    }
}
