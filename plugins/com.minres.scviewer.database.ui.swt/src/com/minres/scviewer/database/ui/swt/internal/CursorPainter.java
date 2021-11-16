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
package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.database.ui.swt.Constants;

public class CursorPainter implements IPainter, ICursor {

	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;
	
	private long time;

	private boolean isDragging;
	
	public final int id;
	
	/// maximum visible canvas position in canvas coordinates
	int maxPosX;
	/// maximum visible position in waveform coordinates
	int maxValX;
	
	/**
	 * @param i 
	 * @param txDisplay
	 */
	public CursorPainter(WaveformCanvas txDisplay, long time, int id) {
		this.waveCanvas = txDisplay;
		this.time=time;
		this.id=id;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isDragging() {
		return isDragging;
	}

	public void setDragging(boolean isDragging) {
		this.isDragging = isDragging;
	}

	
	public void paintArea(Projection proj, Rectangle clientRect) {
		Rectangle area = proj.unProject(clientRect);
		if(!waveCanvas.painterList.isEmpty()){
			
			long scaleFactor=waveCanvas.getScaleFactor();
			long beginPos = area.x;
			
			maxPosX = area.x + area.width;
			maxValX = maxPosX;
	        
			// x position of marker in pixels on canvas
			int x = (int) (time/scaleFactor);
			// distance of marker from the top of Canvas' painting area
			int top = id<0?area.y:area.y+15;
			Color drawColor=waveCanvas.styleProvider.getColor(id<0?WaveformColors.CURSOR:WaveformColors.MARKER);
			Color dragColor = waveCanvas.styleProvider.getColor(WaveformColors.CURSOR_DRAG);
			Color textColor=waveCanvas.styleProvider.getColor(id<0?WaveformColors.CURSOR_TEXT:WaveformColors.MARKER_TEXT);
			if(x>=beginPos && x<=maxValX){
				proj.setForeground(isDragging?dragColor:drawColor);
				proj.drawLine(x, top, x, area.y+area.height);
				proj.setBackground(drawColor);
				proj.setForeground(textColor);
				double dTime=time;
				proj.drawText(Constants.TIME_FORMAT[waveCanvas.getZoomLevel()].format(dTime/waveCanvas.getScaleFactorPow10())+waveCanvas.getUnitStr(), x+1, top);
			}
		}
	}	
}