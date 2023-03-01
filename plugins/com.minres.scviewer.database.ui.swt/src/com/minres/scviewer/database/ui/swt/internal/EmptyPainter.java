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

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class EmptyPainter extends TrackPainter {
	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;

	private static final JPanel DUMMY_PANEL = new JPanel();

	public EmptyPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
	}

	public void paintArea(Projection proj, Rectangle area) {
		Color bgColor = trackEntry.selected?this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_HIGHLITE):this.waveCanvas.styleProvider.getColor(even ? WaveformColors.TRACK_BG_EVEN : WaveformColors.TRACK_BG_ODD);
		GC gc = proj.getGC();
		proj.setBackground(bgColor);
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);
		Color drawColor = this.waveCanvas.styleProvider.getColor(WaveformColors.SEPARATOR);
		int trackHeight=trackEntry.height;
		int txBase=trackHeight*2/5;
		int txHeight=trackHeight/5;
		proj.setBackground(drawColor);
		proj.fillRectangle(new Rectangle(area.x, area.y+txBase,	area.width, txHeight));

		String label = trackEntry.waveform.getName();
		if(label.length()>0) {
			Color textColor=waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL_TEXT);
			FontData fd = gc.getFont().getFontData()[0];
			int height = gc.getDevice().getDPI().y * fd.getHeight() / 72;
			java.awt.Font tmpAwtFont = new java.awt.Font(fd.getName(), fd.getStyle(), height);
			int width = DUMMY_PANEL.getFontMetrics(tmpAwtFont).stringWidth(label);

			int xBegin = (area.width-width)/2-5;
			xBegin = xBegin<0?0:xBegin;
			int xEnd = (area.width+width)/2+5;
			xEnd = xEnd>area.width?area.width:xEnd;
			int yOffsetT = this.waveCanvas.styleProvider.getTrackHeight() / 5 + area.y;
			int yOffsetM = this.waveCanvas.styleProvider.getTrackHeight() / 2 + area.y;
			int yOffsetB = 4 * this.waveCanvas.styleProvider.getTrackHeight() / 5 + area.y;
			int[] points = { 
					xBegin,     yOffsetM, 
					xBegin + 1, yOffsetT, 
					xEnd - 1,   yOffsetT, 
					xEnd,       yOffsetM, 
					xEnd - 1,   yOffsetB, 
					xBegin + 1, yOffsetB
			};
			gc.setBackground(bgColor);
			gc.fillPolygon(points);
			gc.setForeground(drawColor);
			gc.drawPolygon(points);
			Rectangle old = gc.getClipping();
			gc.setForeground(textColor);
			gc.setClipping(xBegin + 3, yOffsetT, xEnd - xBegin - 5, yOffsetB - yOffsetT);
			gc.drawText(label, xBegin + 3, yOffsetM - height / 2 - 1);
			gc.setClipping(old);
		}
	}
}
