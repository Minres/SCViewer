/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.internal;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class TrackAreaPainter implements IPainter {

	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;

	final TreeMap<Integer, IWaveformPainter> trackVerticalOffset;
	/**
	 * @param txDisplay
	 */
	public TrackAreaPainter(WaveformCanvas waveCanvas) {
		this.waveCanvas = waveCanvas;
		this.trackVerticalOffset= new TreeMap<>();
	}

	public void paintArea(Projection proj, Rectangle a) {
		Rectangle area = proj.unProject(new Rectangle(a.x, a.y+waveCanvas.rulerHeight, a.width, a.height-waveCanvas.rulerHeight));
		proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_EVEN));
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);
		if(trackVerticalOffset.size()>0){
			Integer firstKey=trackVerticalOffset.floorKey(area.y);
			if(firstKey==null) firstKey=trackVerticalOffset.firstKey();
			Integer lastKey = trackVerticalOffset.floorKey(area.y+area.height);
			//if(lastKey==null) lastKey= trackVerticalOffset.lastKey();
			Rectangle subArea = new Rectangle(area.x, 0, area.width, 0);
			if(lastKey != null && lastKey.equals(firstKey)){
				subArea.y=firstKey;
				IWaveformPainter p = trackVerticalOffset.get(firstKey);
				subArea.height=p.getHeight();
				p.paintArea(proj, subArea);
			}else{
				for(Entry<Integer, IWaveformPainter> entry : trackVerticalOffset.subMap(firstKey, true, lastKey, true).entrySet()){
					subArea.y=entry.getKey();
					subArea.height=entry.getValue().getHeight();
					entry.getValue().paintArea(proj, subArea);
				}
			}
		}
	}

	public IWaveformPainter getPainterFor(TrackEntry e) {
		for (Entry<Integer,IWaveformPainter> entry : trackVerticalOffset.entrySet()) {
			if(entry.getValue().getTrackEntry() == e)
				return entry.getValue();
		}
		return null;
	}
	
	public void addTrackPainter(IWaveformPainter trackPainter){
		trackVerticalOffset.put(trackPainter.getVerticalOffset()+waveCanvas.rulerHeight, trackPainter);

	}

	public int getHeight(){
		if(trackVerticalOffset.size()==0) return 1;
		return trackVerticalOffset.lastKey() + trackVerticalOffset.lastEntry().getValue().getHeight();
	}
}