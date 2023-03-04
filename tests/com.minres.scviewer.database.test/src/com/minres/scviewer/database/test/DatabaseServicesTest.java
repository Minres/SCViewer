/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxEvent;

public class DatabaseServicesTest {


	private IWaveformDb waveformDb;
		
	@Before
	public void setUp() throws Exception {
		waveformDb=TestWaveformDbFactory.getDatabase();
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
		List<IWaveform> waveforms= waveformDb.getAllWaves();
		assertEquals(14,  waveforms.size());
		assertEquals(2,  waveformDb.getChildNodes().size());
		waveforms.stream().filter(s -> s.getName().equals("bus_addr[7:0]")).forEach(s -> {
			EventEntry bus_data_entry = s.getEvents().floorEntry(1400000000L);
			assertEquals("01001111", bus_data_entry.events[0].toString());
		});

		waveforms.stream().filter(s -> s.getName().equals("rw")).forEach(s -> {
			EventEntry rw_entry = s.getEvents().floorEntry(2360000000L);
			assertEquals("1", rw_entry.events[0].toString());
		});

	}

	@Test
	public void testTxSQLite() throws Exception {
		File f = new File("inputs/my_sqldb.txdb").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(0,  waveformDb.getAllWaves().size());
		assertEquals(0,  waveformDb.getChildNodes().size());
	}

	@Test
	public void testTxText() throws Exception {
		File f = new File("inputs/my_db.txlog").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(3400000000l, waveformDb.getMaxTime());
		List<IWaveform> waveforms =  waveformDb.getAllWaves();
		assertEquals(8,  waveforms.size());
		assertEquals(1,  waveformDb.getChildNodes().size());
		for(IWaveform w:waveforms) {
			if(w.getId()==1) {
				assertEquals(2, w.getRowCount());
			} else if(w.getId()==2l) {
				assertEquals(1, w.getRowCount());
			} else if(w.getId()==3l) {
				assertEquals(1, w.getRowCount());
			}
		}
	}

	@Test
	public void testTxTextTruncated() throws Exception {
		File f = new File("inputs/my_db_truncated.txlog").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(8,  waveformDb.getAllWaves().size());
		assertEquals(1,  waveformDb.getChildNodes().size());
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

	@Test
	public void testFtr() throws Exception {
		File f = new File("inputs/my_db.ftr").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		assertEquals(3400000000l, waveformDb.getMaxTime());
		List<IWaveform> waveforms =  waveformDb.getAllWaves();
		assertEquals(8,  waveforms.size());
		assertEquals(1,  waveformDb.getChildNodes().size());
		for(IWaveform w:waveforms) {
			if(w.getId()==1) {
				assertEquals(2, w.getRowCount());
			} else if(w.getId()==2l) {
				assertEquals(1, w.getRowCount());
			} else if(w.getId()==3l) {
				assertEquals(1, w.getRowCount());
			}
		}
		waveforms.stream().filter(s -> s.getId()==1).forEach(s -> {
			assertEquals(27, s.getEvents().size());
		});
		waveforms.stream().filter(s -> s.getId()==1).map(s -> s.getEventsAtTime(0)).forEach(el ->  {
			assertEquals(1, el.length);
			IEvent evt = el[0];
			assertTrue(evt instanceof ITxEvent);
			ITx tx = ((ITxEvent)evt).getTransaction();
			assertNotNull(tx);
			assertEquals(0, tx.getBeginTime());
			assertEquals(280000000, tx.getEndTime());
			List<ITxAttribute> attr = tx.getAttributes();
			assertEquals(3, attr.size());
		});
	}
	
	@Test
	public void testCFtr() throws Exception {
		File f = new File("inputs/my_db_c.ftr").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		List<IWaveform> waveforms =  waveformDb.getAllWaves();
		assertEquals(3400000000l, waveformDb.getMaxTime());
		assertEquals(8,  waveforms.size());
		assertEquals(1,  waveformDb.getChildNodes().size());
		for(IWaveform w:waveforms) {
			if(w.getId()==1) {
				assertEquals(2, w.getRowCount());
			} else if(w.getId()==2l) {
				assertEquals(1, w.getRowCount());
			} else if(w.getId()==3l) {
				assertEquals(1, w.getRowCount());
			}
		}
		waveforms.stream().filter(s -> s.getId()==1).forEach(s -> {
			assertEquals(27, s.getEvents().size());
		});
		waveforms.stream().filter(s -> s.getId()==1).map(s -> s.getEventsAtTime(0)).forEach(el ->  {
			assertEquals(1, el.length);
			IEvent evt = el[0];
			assertTrue(evt instanceof ITxEvent);
			ITx tx = ((ITxEvent)evt).getTransaction();
			assertNotNull(tx);
			assertEquals(0, tx.getBeginTime());
			assertEquals(280000000, tx.getEndTime());
			List<ITxAttribute> attr = tx.getAttributes();
			assertEquals(3, attr.size());
		});
	}

	@Test
	public void testFst() throws Exception {
		File f = new File("inputs/my_db.fst").getAbsoluteFile();
		assertTrue(f.exists());
		waveformDb.load(f);
		assertNotNull(waveformDb);
		List<IWaveform> waveforms= waveformDb.getAllWaves();
		assertEquals(14,  waveforms.size());
		assertEquals(2,  waveformDb.getChildNodes().size());
		waveforms.stream().filter(s -> s.getName().equals("bus_addr[7:0]")).forEach(s -> {
			EventEntry bus_data_entry = s.getEvents().floorEntry(1400000000L);
			assertEquals("01001111", bus_data_entry.events[0].toString());
		});

		waveforms.stream().filter(s -> s.getName().equals("rw")).forEach(s -> {
			EventEntry rw_entry = s.getEvents().floorEntry(2360000000L);
			assertEquals("1", rw_entry.events[0].toString());
		});
	}
}
