/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.TxIntervalIndex;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class StreamPainter extends TrackPainter{

	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;
	private IWaveform stream;
	private int txBase;
	private int txHeight;

	public StreamPainter(WaveformCanvas waveCanvas, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = waveCanvas;
		this.stream=trackEntry.waveform;
	}

	public void paintArea(Projection proj, Rectangle area) {
		int trackHeight=trackEntry.height/waveCanvas.rows(stream);
		if(waveCanvas.events(stream).size()==0) {
			proj.setFillRule(SWT.FILL_EVEN_ODD);
			proj.setLineStyle(SWT.LINE_SOLID);
			proj.setLineWidth(1);
			proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE));
			for( int y1=area.y+trackHeight/2; y1<area.y+trackEntry.height; y1+=trackHeight)
				proj.drawLine(area.x, y1, area.x+area.width, y1);
			return;
		}
		txBase=trackHeight/5;
		txHeight=trackHeight*3/5;
		if(trackEntry.selected) {
			proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_HIGHLITE));
		}
		else
			proj.setBackground(this.waveCanvas.styleProvider.getColor(even?WaveformColors.TRACK_BG_EVEN:WaveformColors.TRACK_BG_ODD));
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);

		long scaleFactor = this.waveCanvas.getScale();
		long beginPos = area.x;
		long beginTime = beginPos*scaleFactor;
		long endTime = beginTime + area.width*scaleFactor;

		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.setLineStyle(SWT.LINE_SOLID);
		proj.setLineWidth(1);
		proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE));

		for( int y1=area.y+trackHeight/2; y1<area.y+trackEntry.height; y1+=trackHeight)
			proj.drawLine(area.x, y1, area.x+area.width, y1);
		ITxEvent highlighted = null;
        TxIntervalIndex index = getIntervalIndex();
        if (index == null) return;
        for (ITxEvent event : index.overlapping(beginTime, endTime)) {
            ITx tx = event.getTransaction();
            drawTx(proj, area, tx, event.getRowIndex(), false);
            if (waveCanvas.currentSelectedTx != null && waveCanvas.currentSelectedTx.getId() == tx.getId())
                highlighted = event;
        }
        if (highlighted != null) {
            proj.setForeground(waveCanvas.styleProvider.getColor(WaveformColors.LINE_HIGHLITE));
            drawTx(proj, area, highlighted.getTransaction(), highlighted.getRowIndex(), true);
        }
    }

    private TxIntervalIndex getIntervalIndex() { return waveCanvas.intervals(stream); }

	protected void drawTx(Projection proj, Rectangle area, ITx tx, int concurrencyIndex, boolean highlighted ) {
		// compute colors
		IWaveform generator = tx.getGenerator();
		Color[] transColor = waveCanvas.styleProvider.computeColor( generator!=null?generator.getName():"unknown");

		proj.setBackground(transColor[highlighted?1:0]);

		int offset = concurrencyIndex*this.waveCanvas.styleProvider.getTrackHeight();
		Rectangle bb = new Rectangle(
				(int)(tx.getBeginTime()/this.waveCanvas.getScale()), area.y+offset+txBase,
				(int)((tx.getEndTime()-tx.getBeginTime())/this.waveCanvas.getScale()), txHeight);

		if(bb.x+bb.width<area.x || bb.x>area.x+area.width) return;
		if(bb.width==0){
			proj.drawLine(bb.x, bb.y, bb.x, bb.y+bb.height);
		} else {
			if(bb.x < area.x) {
				bb.width = bb.width-(area.x-bb.x)+5;
				bb.x=area.x-5;
			}
			int bbX2 = bb.x+bb.width;
			int areaX2 = area.x+area.width;
			if(bbX2>areaX2){
				bbX2=areaX2+5;
				bb.width= bbX2-bb.x;
			}
			int arc = bb.width<10?1:5;
			proj.fillRoundRectangle(bb.x, bb.y, bb.width, bb.height, arc, arc);
			proj.drawRoundRectangle(bb.x, bb.y, bb.width, bb.height, arc, arc);
		}
	}

	public ITx getClicked(Point point) {
		int lane=point.y/waveCanvas.styleProvider.getTrackHeight();
		long scale = waveCanvas.getScale();
        long time = (long) point.x * scale;
        long low = Math.max(0L, (long) point.x - 5) * scale;
        long high = ((long) point.x + 5) * scale;
        TxIntervalIndex index = getIntervalIndex();
        if (index == null) return null;
        for (ITxEvent event : index.overlapping(low, high)) {
            if (event.getRowIndex() != lane) continue;
            ITx tx = event.getTransaction();
            if (event.getKind() == EventKind.SINGLE ||
                    (tx.getBeginTime() <= time && tx.getEndTime() >= time)) return tx;
        }
		return null;
	}

	public IWaveform getStream() {
		return stream;
	}

	public void setStream(IWaveform stream) {
		this.stream = stream;
	}

	protected ITx getTxFromEntry(int lane, int offset, IEvent[] firstTx) {
		long timePoint=offset*waveCanvas.getScale();
		long timePointLow=(offset-5)*waveCanvas.getScale();
		long timePointHigh=(offset+5)*waveCanvas.getScale();
		for(IEvent e:firstTx){
			if(e instanceof ITxEvent) {
				ITxEvent evt = (ITxEvent) e;
				ITx tx=evt.getTransaction();
				if(
						(evt.getKind()==EventKind.SINGLE && evt.getTime()==timePoint) ||
						(evt.getKind()==EventKind.SINGLE && evt.getTime()>timePointLow && evt.getTime()<timePointHigh) ||
						(evt.getKind()==EventKind.BEGIN && evt.getRowIndex()==lane && evt.getTime()<=timePoint && tx.getEndTime()>=timePoint)
						){
					return tx;
				} 
			}
		}
		return null;
	}

}