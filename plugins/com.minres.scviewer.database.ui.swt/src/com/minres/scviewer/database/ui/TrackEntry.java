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


import com.minres.scviewer.database.IWaveform;

public class TrackEntry {

	IWaveformStyleProvider styleProvider;
	
	public enum ValueDisplay {
		DEFAULT, SIGNED, UNSIGNED

	}

	public enum WaveDisplay {
		DEFAULT, STEP_WISE, CONTINOUS
	}

	final public IWaveform waveform;

	public int vOffset;
	
	public int height;

	public boolean selected;
	
	public String currentValue="";
	
	public ValueDisplay valueDisplay = ValueDisplay.DEFAULT;
	
	public WaveDisplay waveDisplay = WaveDisplay.DEFAULT;
	
	public TrackEntry(IWaveform waveform, IWaveformStyleProvider styleProvider) {
		this.waveform = waveform;
		this.styleProvider=styleProvider;
		vOffset=0;
		height=0;
		selected=false;
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