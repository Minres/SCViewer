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
package com.minres.scviewer.database.ui.swt.internal;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wb.swt.SWTResourceManager;

public class RulerPainter implements IPainter {
    protected WaveformCanvas waveCanvas;
    
    static final int rulerTickMinorC = 10;
    static final int rulerTickMajorC = 100;
       
	static final DecimalFormat df = new DecimalFormat("#.00####"); 

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
        String unit=waveCanvas.getUnitStr();
        int unitMultiplier=waveCanvas.getUnitMultiplier();
        long scaleFactor=waveCanvas.getScaleFactor();

        long startPos=area.x*scaleFactor; 
        long startVal=startPos - proj.getTranslation().x*scaleFactor;
        long endPos=startPos+area.width*scaleFactor;

        long rulerTickMinor = rulerTickMinorC*scaleFactor;
        long rulerTickMajor = rulerTickMajorC*scaleFactor;

        int minorTickY = waveCanvas.rulerHeight-5;
        int majorTickY = waveCanvas.rulerHeight-15;
        int textY=waveCanvas.rulerHeight-20;
        int baselineY=waveCanvas.rulerHeight - 1;
        int bottom=waveCanvas.rulerHeight - 2;

        long modulo = startVal % rulerTickMinor;
        long startMinorIncrPos = startPos+rulerTickMinor-modulo;
        long startMinorIncrVal = startVal+rulerTickMinor-modulo;
        
        gc.setBackground(waveCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gc.fillRectangle(new Rectangle(area.x, area.y, area.width, waveCanvas.rulerHeight));
        gc.setBackground(headerBgColor);
        gc.fillRectangle(new Rectangle(area.x, area.y, area.width, baselineY));
        gc.setForeground(headerFgColor);
        gc.drawLine(area.x, area.y+bottom, area.x+area.width, area.y+bottom);
        
        for (long pos = startMinorIncrPos, tick = startMinorIncrVal; pos < endPos; pos+= rulerTickMinor, tick += rulerTickMinor) {
            int x0_pos = (int) (pos/scaleFactor);
            long x0_val = tick/scaleFactor;
            if ((tick % rulerTickMajor) == 0) {
                gc.drawText(df.format(x0_val*unitMultiplier)+unit, x0_pos, area.y+textY);
                gc.drawLine(x0_pos, area.y+majorTickY, x0_pos,area.y+ bottom);
            } else {
                gc.drawLine(x0_pos, area.y+minorTickY, x0_pos, area.y+bottom);
            }
        }
    }
}
