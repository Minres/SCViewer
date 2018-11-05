/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxEvent.Type;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;
import com.minres.scviewer.database.vcd.VCDSignal;

public class DatabaseServicesTest {


	private static IWaveformDbFactory waveformDbFactory;

	private IWaveformDb waveformDb;
	
	public synchronized void setFactory(IWaveformDbFactory service) {
		waveformDbFactory = service;
	}

	public synchronized void unsetFactory(IWaveformDbFactory service) {
		if (waveformDbFactory == service) {
			waveformDbFactory = null;
		}
	}
	
	@Before
	public void setUp() throws Exception {
		waveformDb=waveformDbFactory.getDatabase();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testVCD() throws Exception {
		File f = new File("inputs/my_db.vcd").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		List<IWaveform> waves= waveformDb.getAllWaves();
		assertEquals(14,  waves.size());
		assertEquals(2,  waveformDb.getChildNodes().size());
		IWaveform bus_data_wave = waves.get(0);
		VCDSignal<?> bus_data_sig = (VCDSignal<?>) bus_data_wave;
		Entry<Long, ?> bus_data_entry = bus_data_sig.getEvents().floorEntry(1400000000L);
		assertTrue("01111000".equals(bus_data_entry.getValue().toString()));
		IWaveform rw_wave = waves.get(2);
		VCDSignal<?> rw_sig = (VCDSignal<?>) rw_wave;
		Entry<Long, ?> rw_entry = rw_sig.getEvents().floorEntry(2360000000L);
		assertTrue("1".equals(rw_entry.getValue().toString()));
	}

	@Test
	public void testTxSQLite() throws Exception {
		File f = new File("inputs/my_sqldb.txdb").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(3,  waveformDb.getAllWaves().size());
		assertEquals(1,  waveformDb.getChildNodes().size());
	}

	@Test
	public void testTxText() throws Exception {
		File f = new File("inputs/my_db.txlog").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(3,  waveformDb.getAllWaves().size());
		assertEquals(1,  waveformDb.getChildNodes().size());
	}

	@Test
	public void testTxLDb() throws Exception {
		File f = new File("inputs/my_ldb.txldb").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(1,  waveformDb.getChildNodes().size());
		List<IWaveform> waves = waveformDb.getAllWaves();
		assertEquals(3,  waves.size());
		IWaveform wave = waves.get(0);
		assertTrue(wave instanceof ITxStream<?>);
		ITxStream<?> stream = (ITxStream<?>) wave;
		assertEquals(2,  stream.getGenerators().size());
		NavigableMap<Long, List<ITxEvent>> eventsList = stream.getEvents();
		assertEquals(27, eventsList.size());
		Entry<Long, List<ITxEvent>> eventEntry = eventsList.firstEntry();
		assertEquals(100000000L, (long) eventEntry.getKey());
		List<ITxEvent> events = eventEntry.getValue();
		assertEquals(1, events.size());
		ITxEvent event = events.get(0);
		assertEquals(Type.BEGIN, event.getType());
		ITx tx = event.getTransaction();
		assertEquals(3L,  (long) tx.getId());
		List<ITxAttribute> attrs = tx.getAttributes();
		assertEquals(1, attrs.size());
		ITxAttribute attr = attrs.get(0);
		assertEquals("data", attr.getName());
		assertEquals(DataType.UNSIGNED, attr.getDataType());
		assertEquals(AssociationType.END, attr.getType());
		assertTrue(attr.getValue() instanceof Integer);
		assertEquals(0, (int) attr.getValue());
	}

	@Test
	public void testHierarchicalVCD() throws Exception {
		File f = new File("inputs/simple_system.vcd").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(779,  waveformDb.getAllWaves().size());
		assertEquals(1,  waveformDb.getChildNodes().size());
	}


}
