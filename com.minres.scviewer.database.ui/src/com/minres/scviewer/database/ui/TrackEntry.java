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
package com.minres.scviewer.database.ui;


import java.awt.Color;

import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveform;

public class TrackEntry {

 	// color info
	public static Color fallbackColor = new Color(200,0,0);
	public static Color highlightedFallbackColor = new Color(255,0,0);
	private Color[]signalColors;
	
	// list of random colors
	private static Color[][] randomColors = {
			{ new Color( 170,  66,  37 ), new Color ( 190,  66,  37 ) },
			{ new Color(  96,  74, 110 ), new Color (  96,  74, 130 ) },
			{ new Color( 133, 105, 128 ), new Color ( 153, 105, 128 ) },
			{ new Color(   0, 126, 135 ), new Color (   0, 126, 155 ) },
			{ new Color( 243, 146,  75 ), new Color ( 255, 146,  75 ) },
			{ new Color( 206, 135, 163 ), new Color ( 226, 135, 163 ) },
			{ new Color( 124, 103,  74 ), new Color ( 144, 103,  74 ) },
			{ new Color( 194, 187, 169 ), new Color ( 214, 187, 169 ) },
			{ new Color( 104,  73,  71 ), new Color ( 124,  73,  71 ) },
			{ new Color(  75, 196, 213 ), new Color (  75, 196, 233 ) },
			{ new Color( 206, 232, 229 ), new Color ( 206, 252, 229 ) },
			{ new Color( 169, 221, 199 ), new Color ( 169, 241, 199 ) },
			{ new Color( 100, 165, 197 ), new Color ( 100, 165, 217 ) },
			{ new Color( 150, 147, 178 ), new Color ( 150, 147, 198 ) },
			{ new Color( 200, 222, 182 ), new Color ( 200, 242, 182 ) },
			{ new Color( 147, 208, 197 ), new Color ( 147, 228, 197 ) }
	};
	
	public static Color[] computeColor (String streamValue, Color fallback, Color highlightedFallback) {
	
		Color[]result = new Color[2];
				
		result[0] = fallback;
		result[1] = highlightedFallback;
		
		// assign colors to standard values
		if (streamValue.contains("read")){
			result[0] = new Color(86,174,53);
			result[1] = new Color (86,194,53);
		}else if (streamValue.contains("rdata")){
			result[0] = new Color(138,151,71);
			result[1] = new Color (138,171,71);
		}else if (streamValue.contains("addr")){
			result[0] = new Color(233,187,68);
			result[1] = new Color (233,207,68);
		}else if (streamValue.contains("write")){
			result[0] = new Color(1,128,191);
			result[1] = new Color (1,128,211);
		}else if (streamValue.contains("wdata")){
			result[0] = new Color(2,181,160);
			result[1] = new Color (2,201,160);
			
		}else {
			// assign "random" color here, one name always results in the same color!
			if( randomColors.length > 0 ) {
				int index = Math.abs(streamValue.hashCode()) % randomColors.length;
				result[0] = randomColors[index][0];
				result[1] = randomColors[index][1];
			}
		}
		
		return result;
		
	}
	
	public void setColor(Color changedColor, Color highlightColor) {
		signalColors[0] = changedColor;
		signalColors[1] = highlightColor;
	}
	
	public Color[] getColors() {
		return signalColors;
	}
	
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
	
	public TrackEntry(IWaveform waveform) {
		this.waveform = waveform;
		vOffset=0;
		height=0;
		selected=false;
		signalColors = new Color[2];
		signalColors[0] = fallbackColor;
		signalColors[1] = highlightedFallbackColor;
	}
	
	public boolean isStream(){
		return waveform instanceof ITxStream<?>;
	}

	public ITxStream<? extends ITxEvent> getStream(){
		return (ITxStream<?>) waveform;
	}

	public boolean isSignal(){
		return waveform instanceof ISignal<?>;
	}
	
	public ISignal<?> getSignal(){
		return (ISignal<?>) waveform;
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