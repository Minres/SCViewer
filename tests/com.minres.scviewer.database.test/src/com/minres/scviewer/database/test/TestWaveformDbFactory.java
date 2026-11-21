/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.test;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

public class TestWaveformDbFactory {
	private static IWaveformDbFactory waveformDbFactory;

	
	public synchronized void setFactory(IWaveformDbFactory service) {
		waveformDbFactory = service;
	}

	public synchronized void unsetFactory(IWaveformDbFactory service) {
		if (waveformDbFactory == service) {
			waveformDbFactory = null;
		}
	}
	
	public static IWaveformDb getDatabase() throws Exception {
		return waveformDbFactory.getDatabase();
	}


}
