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
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class BlankPainter extends TrackPainter {
	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;

	public BlankPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
	}

	public void paintArea(Projection proj, Rectangle area) {
		if (trackEntry.selected)
			proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_HIGHLITE));
		else
			proj.setBackground(this.waveCanvas.styleProvider.getColor(even ? WaveformColors.TRACK_BG_EVEN : WaveformColors.TRACK_BG_ODD));
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);
		int trackHeight=trackEntry.height;
		int txBase=trackHeight*2/5;
		int txHeight=trackHeight/5;
		Rectangle bb = new Rectangle(area.x, area.y+txBase,	area.width, txHeight);
		proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.BLANK));
		proj.fillRectangle(bb);

	}
}
