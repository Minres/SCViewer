package com.minres.scviewer.database.test;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

public class DatabaseFactory {
	private static IWaveformDbFactory waveformDbFactory;
	
	public synchronized void setFactory(IWaveformDbFactory service) {
		waveformDbFactory = service;
	}

	public synchronized void unsetFactory(IWaveformDbFactory service) {
		if (waveformDbFactory == service) {
			waveformDbFactory = null;
		}
	}

	public IWaveformDb getDatabase() {
		return waveformDbFactory.getDatabase();
	}
	
	
}
