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
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

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
		Entry<Long, IEvent[]> bus_data_entry = bus_data_wave.getEvents().floorEntry(1400000000L);
		assertTrue("01111000".equals(bus_data_entry.getValue()[0].toString()));
		IWaveform rw_wave = waves.get(2);
		Entry<Long, IEvent[]> rw_entry = rw_wave.getEvents().floorEntry(2360000000L);
		assertTrue("1".equals(rw_entry.getValue()[0].toString()));
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
	public void testTxTextTruncated() throws Exception {
		File f = new File("inputs/my_db_truncated.txlog").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(3,  waveformDb.getAllWaves().size());
		assertEquals(1,  waveformDb.getChildNodes().size());
	}

	//@Test
	public void testTxLDb() throws Exception {
		File f = new File("inputs/my_ldb.txldb").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(1,  waveformDb.getChildNodes().size());
		List<IWaveform> waves = waveformDb.getAllWaves();
		assertEquals(3,  waves.size());
		IWaveform stream = waves.get(0);
		NavigableMap<Long, IEvent[]> eventsList = stream.getEvents();
		assertEquals(27, eventsList.size());
		Entry<Long, IEvent[]> eventEntry = eventsList.firstEntry();
		assertEquals(100000000L, (long) eventEntry.getKey());
		IEvent[] events = eventEntry.getValue();
		assertEquals(1, events.length);
		IEvent event = events[0];
		assertEquals(EventKind.BEGIN, event.getKind());
		assertTrue(event instanceof ITxEvent);
		ITx tx = ((ITxEvent)event).getTransaction();
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
