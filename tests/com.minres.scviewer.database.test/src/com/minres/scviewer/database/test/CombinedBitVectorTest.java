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
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Proxy;
import org.junit.Test;
import com.minres.scviewer.database.*;
import com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit;


public class CombinedBitVectorTest {
    private static class Signal extends EmptyWaveform {
        private final int width;
        private final IEventList values = new EventList();
        Signal(int id, String name, int width) { super(name); this.width = width; }
        void addSignalChange(long time, BitVector value) { values.put(time, value); }
        @Override public int getWidth() { return width; }
        @Override public WaveformType getType() { return WaveformType.SIGNAL; }
        @Override public IEventList getEvents() { return values; }
        @Override public IEvent[] getEventsAtTime(long time) { return values.get(time); }
        @Override public IEvent[] getEventsBeforeTime(long time) {
            var entry = values.floorEntry(time); return entry == null ? new IEvent[0] : entry.events;
        }
    }
    private Signal signal(String name, long[] times, String... values) {
        var signal = new Signal(0, name, 1);
        for (int i = 0; i < times.length; i++) signal.addSignalChange(times[i], BitVector.fromString(1, values[i]));
        return signal;
    }
    private CombinedBitVectorWaveform combine(IWaveform a, IWaveform b) {
        return new CombinedBitVectorWaveform("bus", List.of(new SourceBit(a, 0), new SourceBit(b, 0)));
    }
    private String value(EventEntry entry) { return entry == null ? null : entry.events[0].toString(); }

    @Test public void cancellationStopsAnUnchangedHistoryScanAndAllowsRetry() {
        var input = new Signal(0, "input", 2);
        for (int i = 0; i < 1000; i++) input.addSignalChange(i, BitVector.fromString(2, i % 2 == 0 ? "00" : "10"));
        var bus = combine(input, input);
        AtomicInteger checks = new AtomicInteger();
        try {
            ReadCancellation.with(() -> checks.incrementAndGet() > 30, () -> bus.getEvents().floorEntry(999));
            fail("History scan ignored cancellation");
        } catch (java.util.concurrent.CancellationException expected) { }
        assertEquals(0, bus.getEvents().floorEntry(999).timestamp);
    }

    @Test public void mergesStaggeredAndSimultaneousChangesAndSuppressesUnchangedValues() {
        var a = signal("a", new long[]{5, 20, 20, 25, 30, 40}, "0", "0", "1", "1", "x", "z");
        var b = signal("b", new long[]{10, 20, 25}, "1", "0", "0");
        var bus = combine(a, b);
        var map = bus.getEvents();
        assertEquals(List.of(5L, 10L, 20L, 30L, 40L), map.entrySet().stream().map(e -> e.timestamp).toList());
        assertEquals(List.of("0x", "01", "10", "x0", "z0"), map.entrySet().stream().map(this::value).toList());
        assertEquals(0, bus.getEventsBeforeTime(4).length);
        assertEquals("01", bus.getEventsBeforeTime(19)[0].toString());
        assertEquals("10", bus.getEventsBeforeTime(25)[0].toString());
        assertNull(bus.getEventsAtTime(25));
        assertEquals(30, map.higherEntry(20).timestamp);
        assertEquals(20, map.lowerEntry(30).timestamp);
        assertEquals(20, map.floorEntry(25).timestamp);
        assertEquals(30, map.ceilingEntry(25).timestamp);
        assertTrue(map.containsKey(20));
        assertFalse(map.containsKey(25));
        ((BitVector) map.get(20)[0]).setValue(0, BitValue.Z);
        assertEquals("10", bus.getEventsAtTime(20)[0].toString());
        assertEquals("0", b.getEventsAtTime(20)[0].toString());
        try { map.put(0, new BitVector(2)); fail(); } catch (UnsupportedOperationException expected) { }
    }

    @Test public void rangesRemainBoundedAndUseUnsignedTimestamps() {
        var a = signal("a", new long[]{0, Long.MAX_VALUE, Long.MIN_VALUE, -1L}, "0", "1", "x", "z");
        var b = signal("b", new long[]{0}, "1");
        var map = combine(a, b).getEvents();
        assertEquals(0, map.firstKey()); assertEquals(-1L, map.lastKey());
        assertEquals(Long.MIN_VALUE, map.higherEntry(Long.MAX_VALUE).timestamp);
        assertEquals(Long.MAX_VALUE, map.lowerEntry(Long.MIN_VALUE).timestamp);
        var range = map.subMap(Long.MAX_VALUE, false, -1L);
        assertEquals(2, range.size());
        assertNull(range.floorEntry(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, range.ceilingEntry(0).timestamp);
        assertEquals(-1L, range.floorEntry(-1L).timestamp);
        assertNull(range.higherEntry(-1L));
        assertEquals(2, range.subMap(0, true, -1L).size());
        assertTrue(map.subMap(0, false, 0).isEmpty());
        assertTrue(range.subMap(0, true, Long.MAX_VALUE).isEmpty());
        try { map.subMap(-1L, true, 0); fail(); } catch (IllegalArgumentException expected) { }
    }

    @Test public void handlesEmptyInputsAndMissingInitialValues() {
        var empty = signal("empty", new long[]{});
        var other = signal("other", new long[]{7, 9}, "x", "z");
        var bus = combine(empty, other);
        assertEquals("xx", value(bus.getEvents().firstEntry()));
        assertEquals("xz", value(bus.getEvents().lastEntry()));
        assertEquals(2, bus.getEvents().size());
        var map = combine(empty, empty).getEvents();
        assertTrue(map.isEmpty()); assertEquals(0, map.size());
        assertNull(map.firstEntry()); assertNull(map.lastEntry());
        assertNull(map.floorEntry(-1L)); assertNull(map.ceilingEntry(0));
        try { map.firstKey(); fail(); } catch (NoSuchElementException expected) { }
        try { map.lastKey(); fail(); } catch (NoSuchElementException expected) { }
    }

    @Test public void wideVectorsAndCompositionFlattenToRecordedInputsWithIndependentIdentity() {
        var a = signal("a", new long[]{0}, "1");
        var b = signal("b", new long[]{0}, "z");
        List<SourceBit> bits = new ArrayList<>();
        for (int i = 0; i < 80; i++) bits.add(new SourceBit(i % 2 == 0 ? a : b, 0));
        var wide = new CombinedBitVectorWaveform("wide", bits);
        assertEquals("1z".repeat(40), value(wide.getEvents().firstEntry()));
        var combined = new CombinedBitVectorWaveform("bus", List.of(new SourceBit(wide, 1), new SourceBit(wide, 0)));
        assertSame(a, combined.getSources().get(0).waveform());
        assertSame(b, combined.getSources().get(1).waveform());
        assertEquals("1z", value(combined.getEvents().firstEntry()));
        assertFalse(combined.isSame(new CombinedBitVectorWaveform("bus", bits)));
        var restored = new CombinedBitVectorWaveform(combined.getFullName(), "renamed", combined.getSources());
        assertTrue(combined.isSame(restored)); assertEquals("renamed", restored.getDisplayName());
        String id = restored.getFullName(); restored.setName("another name"); assertEquals(id, restored.getFullName());
    }

    @Test public void constructionAndRangeAccessDoNotMaterializeTheRecording() {
        AtomicInteger loads = new AtomicInteger();
        var delegate = signal("a", new long[]{0, 10, 20}, "0", "1", "0").getEvents();
        var source = new Signal(0, "a", 1) {
            @Override public IEventList getEvents() {
                loads.incrementAndGet();
                return (IEventList) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IEventList.class},
                        (proxy, method, args) -> {
                            if (List.of("entrySet", "iterator", "size").contains(method.getName()))
                                throw new AssertionError("Full source traversal is unnecessary");
                            return method.invoke(delegate, args);
                        });
            }
        };
        var bus = combine(source, source);
        var range = bus.getEvents().subMap(5, true, 15);
        assertEquals(0, loads.get());
        assertEquals("11", value(range.firstEntry()));
        assertEquals(2, loads.get());
        assertEquals("11", value(range.lastEntry()));
        assertEquals(2, loads.get());
    }
}
