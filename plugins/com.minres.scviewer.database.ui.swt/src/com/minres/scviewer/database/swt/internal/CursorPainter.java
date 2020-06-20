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
package com.minres.scviewer.database.swt.internal;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.WaveformColors;

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
		if(this.waveCanvas.painterList.size()>0){
			
			long scaleFactor=waveCanvas.getScaleFactor();
			long beginPos = area.x;
			
			maxPosX = area.x + area.width;
			maxValX = maxPosX;
	        
			// x position of marker in pixels on canvas
			int x = (int) (time/scaleFactor);
			// distance of marker from the top of Canvas' painting area
			int top = id<0?area.y:area.y+15;
			Color drawColor=waveCanvas.colors[id<0?WaveformColors.CURSOR.ordinal():WaveformColors.MARKER.ordinal()];
			Color dragColor = waveCanvas.colors[WaveformColors.CURSOR_DRAG.ordinal()];
			Color textColor=waveCanvas.colors[id<0?WaveformColors.CURSOR_TEXT.ordinal():WaveformColors.MARKER_TEXT.ordinal()];
			if(x>=beginPos && x<=maxValX){
				proj.setForeground(isDragging?dragColor:drawColor);
				proj.drawLine(x, top, x, area.y+area.height);
				proj.setBackground(drawColor);
				proj.setForeground(textColor);
				Double dTime=new Double(time);
				proj.drawText((dTime/waveCanvas.getScaleFactorPow10())+waveCanvas.getUnitStr(), x+1, top);
			}
		}
	}	
}