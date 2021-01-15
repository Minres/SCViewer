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
