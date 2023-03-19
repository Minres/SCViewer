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
package com.minres.scviewer.database.ui;


import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IWaveform;

public class TrackEntry {
	public enum HierState { 
		NONE, CLOSED, OPENED
	}

	public enum ValueDisplay {
		DEFAULT, BINARY, SIGNED, UNSIGNED

	}

	public enum WaveDisplay {
		DEFAULT, STEP_WISE, CONTINOUS
	}

	IWaveformStyleProvider styleProvider = null;

	final public IWaveform waveform;

	public int vOffset=0;
	
	public int height=0;

	public boolean selected=false;
	
	public HierState hierState=HierState.NONE;

	public List<TrackEntry> waveforms = new ArrayList<>();

	public String currentValue="";
	
	public ValueDisplay valueDisplay = ValueDisplay.DEFAULT;
	
	public WaveDisplay waveDisplay = WaveDisplay.DEFAULT;
	
	public TrackEntry() {
		this.waveform = null;
		this.styleProvider=null;
	}

	public TrackEntry(IWaveform waveform, IWaveformStyleProvider styleProvider) {
		this.waveform = waveform;
		this.styleProvider=styleProvider;
		this.hierState = (waveform!=null && waveform.getChildNodes().size()>0)?HierState.CLOSED:HierState.NONE;
	}

	public TrackEntry(TrackEntry[] waveform, IWaveformStyleProvider styleProvider) {
		this(new EmptyWaveform(), styleProvider);
		this.hierState = HierState.CLOSED;
		for (TrackEntry iWaveform : waveform) {
			waveforms.add(iWaveform);
		}
	}

	@Override
    public boolean equals(Object obj) {
		if(obj instanceof TrackEntry){
			TrackEntry o = (TrackEntry) obj;
			return waveform==o.waveform && vOffset==o.vOffset;
		}
		return false;
	}
	
}