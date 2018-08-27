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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

public class DatabaseServicesPerformanceTest {

	private DatabaseFactory databaseFactory;

	private IWaveformDb waveformDb;
		
	@Before
	public void setUp() throws Exception {
		databaseFactory = new DatabaseFactory();
		waveformDb=databaseFactory.getDatabase();
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
		assertEquals(14,  waveformDb.getAllWaves().size());
		assertEquals(2,  waveformDb.getChildNodes().size());
	}

	@Test
	public void testTxText() throws Exception {
		File f = new File("inputs/simple_system.txlog").getAbsoluteFile();
		assertTrue(f.exists());
		long timeBefore = System.currentTimeMillis();
		waveformDb.load(f);
		long timeAfter = System.currentTimeMillis();
	    long elapsed = timeAfter - timeBefore;
		assertNotNull(waveformDb);
		System.out.println("elapsed:" + elapsed);
	}


}
