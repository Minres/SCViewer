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
import java.util.List;
import org.junit.Test;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.HierState;

public class TrackGroupingTest {
    @Test
    public void groupingRecordedAndCombinedVectorBitsCopiesThemAfterTheVector() {
        var source = new BitVectorExpansionTest.Signal("bus", 3) {
            @Override public com.minres.scviewer.database.IEventList getEvents() {
                throw new AssertionError("Grouping must not read signal events");
            }
        };
        var combined = new com.minres.scviewer.database.CombinedBitVectorWaveform("combined", List.of(
                new com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit(source, 2),
                new com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit(source, 1),
                new com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit(source, 0)));
        for (var waveform : List.of(source, combined)) {
            TrackEntry bus = new TrackEntry(waveform, null);
            bus.setExpanded(true);
            var bits = List.copyOf(bus.waveforms);
            bits.get(0).valueDisplay = TrackEntry.ValueDisplay.SIGNED;
            bits.get(2).waveDisplay = TrackEntry.WaveDisplay.STEP_WISE;
            TrackEntry tail = new TrackEntry(new EmptyWaveform("tail"), null);
            var roots = new java.util.ArrayList<>(List.of(bus, tail));

            TrackEntry group = TrackEntry.groupTracks(roots, List.of(bits.get(2), bits.get(0)), "bits", null);
            assertEquals(List.of(bus, group, tail), roots);
            assertEquals(bits, bus.waveforms);
            assertEquals(2, group.waveforms.size());
            for (int index = 0; index < 2; index++) {
                TrackEntry original = bits.get(index == 0 ? 0 : 2);
                TrackEntry copy = group.waveforms.get(index);
                assertNotSame(original, copy);
                assertSame(original.waveform, copy.waveform);
                assertEquals(original.valueDisplay, copy.valueDisplay);
                assertEquals(original.waveDisplay, copy.waveDisplay);
            }
            group.waveforms.get(0).valueDisplay = TrackEntry.ValueDisplay.BINARY;
            assertEquals(TrackEntry.ValueDisplay.SIGNED, bits.get(0).valueDisplay);
            assertTrue(TrackEntry.ungroupTracks(roots, group));
            assertEquals(bits, bus.waveforms);
            assertEquals(List.of(bus, group.waveforms.get(0), group.waveforms.get(1), tail), roots);
        }
    }

    @Test
    public void groupingBitsOfNestedVectorPlacesCopiesInItsParentGroup() {
        TrackEntry bus = new TrackEntry(new BitVectorExpansionTest.Signal("bus", 2), null);
        bus.setExpanded(true);
        var bits = List.copyOf(bus.waveforms);
        TrackEntry tail = new TrackEntry(new EmptyWaveform("tail"), null);
        TrackEntry outer = TrackEntry.group("outer", List.of(bus, tail), null);
        var roots = new java.util.ArrayList<>(List.of(outer));
        TrackEntry group = TrackEntry.groupTracks(roots, bits, "bits", null);
        assertEquals(List.of(outer), roots);
        assertEquals(List.of(bus, group, tail), outer.waveforms);
        assertEquals(bits, bus.waveforms);
        bus.setExpanded(false);
        bus.setExpanded(true);
        assertEquals(bits, bus.waveforms);
        assertNotSame(bits.get(0), group.waveforms.get(0));
    }

    @Test
    public void groupingStandaloneBitTracksMovesTheirExistingEntries() {
        var source = new BitVectorExpansionTest.Signal("bus", 2);
        TrackEntry first = new TrackEntry(new com.minres.scviewer.database.ui.BitWaveform(source, 1), null);
        TrackEntry second = new TrackEntry(new com.minres.scviewer.database.ui.BitWaveform(source, 0), null);
        var roots = new java.util.ArrayList<>(List.of(first, second));
        TrackEntry group = TrackEntry.groupTracks(roots, List.of(second, first), "bits", null);
        assertEquals(List.of(group), roots);
        assertSame(first, group.waveforms.get(0));
        assertSame(second, group.waveforms.get(1));
    }

    @Test
    public void deletingNestedSeparatorLeavesItsGroupAndOtherMembersIntact() {
        TrackEntry separator = new TrackEntry(new EmptyWaveform(), null);
        TrackEntry otherSeparator = new TrackEntry(new EmptyWaveform(), null);
        TrackEntry inner = TrackEntry.group("inner", List.of(separator, otherSeparator), null);
        TrackEntry outer = TrackEntry.group("outer", List.of(inner), null);
        var roots = new java.util.ArrayList<>(List.of(outer));
        separator.selected = true;
        TrackEntry.removeTracks(roots, List.of(separator));
        assertEquals(List.of(outer), roots);
        assertEquals(List.of(inner), outer.waveforms);
        assertEquals(List.of(otherSeparator), inner.waveforms);
        assertFalse(separator.selected);
        TrackEntry.removeTracks(roots, List.of(otherSeparator));
        assertTrue(inner.waveforms.isEmpty());
        assertTrue(inner.isGroup());
        assertEquals(List.of(outer, inner), TrackEntry.visibleTracks(roots));
    }

    @Test
    public void deletingMixedDepthSelectionAndSelectedAncestorIsSafe() {
        TrackEntry child = new TrackEntry(new EmptyWaveform("child"), null);
        TrackEntry group = TrackEntry.group("group", List.of(child), null);
        TrackEntry root = new TrackEntry(new EmptyWaveform("root"), null);
        var roots = new java.util.ArrayList<>(List.of(group, root));
        TrackEntry.removeTracks(roots, List.of(child, root));
        assertEquals(List.of(group), roots);
        assertTrue(group.waveforms.isEmpty());
        group.waveforms.add(child);
        TrackEntry.removeTracks(roots, List.of(group, child));
        assertTrue(roots.isEmpty());
    }

    @Test
    public void groupMembersCanBeSeparatorsAndGroupsAtAnyDepth() {
        TrackEntry separator = new TrackEntry(new EmptyWaveform(), null);
        TrackEntry signal = new TrackEntry(new EmptyWaveform("signal"), null);
        TrackEntry inner = TrackEntry.group("inner", List.of(signal), null);
        var roots = new java.util.ArrayList<>(List.of(separator, inner));
        TrackEntry outer = TrackEntry.groupTracks(roots, List.of(inner, separator), "outer", null);
        assertEquals(List.of(outer), roots);
        assertEquals(List.of(separator, inner), outer.waveforms);
        assertFalse(separator.isGroup());
        assertTrue(inner.isGroup());
        // Selection order must not change display order, including nested groups.
        TrackEntry nested = TrackEntry.groupTracks(roots, List.of(inner, separator), "nested", null);
        assertEquals(List.of(nested), outer.waveforms);
        assertEquals(List.of(separator, inner), nested.waveforms);
        assertEquals(List.of(outer, nested, separator, inner, signal), TrackEntry.visibleTracks(roots));
        assertTrue(TrackEntry.ungroupTracks(roots, nested));
        assertEquals(List.of(separator, inner), outer.waveforms);
        assertTrue(TrackEntry.ungroupTracks(roots, inner));
        assertEquals(List.of(separator, signal), outer.waveforms);
    }

    @Test
    public void groupingAcrossParentsOrWithAnAncestorIsRejectedWithoutChanges() {
        TrackEntry child = new TrackEntry(new EmptyWaveform("child"), null);
        TrackEntry group = TrackEntry.group("group", List.of(child), null);
        TrackEntry other = new TrackEntry(new EmptyWaveform("other"), null);
        var roots = new java.util.ArrayList<>(List.of(group, other));
        assertNull(TrackEntry.groupTracks(roots, List.of(group, child), "invalid", null));
        assertNull(TrackEntry.groupTracks(roots, List.of(other, child), "invalid", null));
        assertEquals(List.of(group, other), roots);
        assertEquals(List.of(child), group.waveforms);
    }

    @Test
    public void collapseRetainsHeaderAndChildrenInOrder() {
        TrackEntry first = new TrackEntry(new EmptyWaveform("first"), null);
        TrackEntry second = new TrackEntry(new EmptyWaveform("second"), null);
        TrackEntry group = TrackEntry.group("bus", List.of(first, second), null);
        assertEquals("bus", group.waveform.getFullName());
        assertEquals(List.of(group, first, second), TrackEntry.visibleTracks(List.of(group)));
        group.hierState = HierState.CLOSED;
        assertEquals(List.of(group), TrackEntry.visibleTracks(List.of(group)));
        group.hierState = HierState.OPENED;
        assertEquals(List.of(group, first, second), TrackEntry.visibleTracks(List.of(group)));
    }

    @Test
    public void nestedCollapsePreservesNestedExpansionState() {
        TrackEntry track = new TrackEntry(new EmptyWaveform("track"), null);
        TrackEntry inner = TrackEntry.group("inner", List.of(track), null);
        TrackEntry outer = TrackEntry.group("outer", List.of(inner), null);
        assertEquals(List.of(outer, inner, track), TrackEntry.visibleTracks(List.of(outer)));
        inner.hierState = HierState.CLOSED;
        outer.hierState = HierState.CLOSED;
        outer.hierState = HierState.OPENED;
        assertEquals(List.of(outer, inner), TrackEntry.visibleTracks(List.of(outer)));
        assertSame(track, inner.waveforms.get(0));
        assertFalse(track.isGroup());
    }
}
