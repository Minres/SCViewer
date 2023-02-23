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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wb.swt.SWTResourceManager;

import com.minres.scviewer.database.ui.swt.Constants;

public class RulerPainter implements IPainter {
    protected final WaveformCanvas waveCanvas;
    
    static final int RULER_TICK_MINOR = 10;
    static final int RULER_TICK_MAJOR = 100;
       
    public RulerPainter(WaveformCanvas waveCanvas) {
        this.waveCanvas=waveCanvas;
    }

    @Override
    public void paintArea(Projection proj, Rectangle area) {
    	GC gc = proj.getGC();
    	Color headerFgColor=waveCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    	if(headerFgColor.isDisposed())
    		headerFgColor=SWTResourceManager.getColor(0,0,0);
    	Color headerBgColor = waveCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    	if(headerBgColor.isDisposed())
    		headerBgColor=SWTResourceManager.getColor(255,255,255);
    	
        long scaleFactor=waveCanvas.getScale();
        long startTime=waveCanvas.getMinVisibleTime(); 
        long endTime=waveCanvas.getMaxVisibleTime();

        long multiplier = Constants.POWERS_OF_TEN[waveCanvas.getScaleMagnitude()];
        long rulerTickMinor = RULER_TICK_MINOR*multiplier;
        long rulerTickMajor = RULER_TICK_MAJOR*multiplier;
        if((endTime-startTime)/rulerTickMinor>area.width/5) {
        	rulerTickMinor*=10;
        	rulerTickMajor*=10;
        }
        int minorTickY = waveCanvas.rulerHeight-5;
        int majorTickY = waveCanvas.rulerHeight-15;
        int textY=waveCanvas.rulerHeight-30;
        int baselineY=waveCanvas.rulerHeight - 1;
        int bottom=waveCanvas.rulerHeight - 2;

        long safe_scale_factor = scaleFactor!=0?scaleFactor:1;
        long startTickTime = startTime+rulerTickMinor-(startTime % rulerTickMinor);
        long majorTickDist = rulerTickMajor/safe_scale_factor;
        
        gc.setBackground(waveCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gc.fillRectangle(new Rectangle(area.x, area.y, area.width, waveCanvas.rulerHeight));
        gc.setBackground(headerBgColor);
        gc.fillRectangle(new Rectangle(area.x, area.y, area.width, baselineY));
        gc.setForeground(headerFgColor);
        gc.drawLine(area.x, area.y+bottom, area.x+area.width, area.y+bottom);
        int maxTextLength=0;
        for (long tickTime = startTickTime; tickTime < endTime; tickTime+= rulerTickMinor) {
            if ((tickTime % rulerTickMajor) == 0) {
            	Point textSize = gc.textExtent(waveCanvas.timeToString(tickTime));
            	maxTextLength=textSize.x>maxTextLength?textSize.x:maxTextLength;
            }
        }
        boolean drawEvery = majorTickDist>maxTextLength;
        boolean drawText=true;
        for (long tickTime = startTickTime; tickTime < endTime; tickTime+= rulerTickMinor) {
            int x0Pos = (int) (tickTime/safe_scale_factor) + proj.getTranslation().x;
            if ((tickTime % rulerTickMajor) == 0) {
            	if(drawEvery ||  drawText)
            		gc.drawText(waveCanvas.timeToString(tickTime), x0Pos, area.y+textY);
                gc.drawLine(x0Pos, area.y+majorTickY, x0Pos,area.y+ bottom);
                drawText=!drawText;
            } else {
                gc.drawLine(x0Pos, area.y+minorTickY, x0Pos, area.y+bottom);
            }
        }
    }
}
