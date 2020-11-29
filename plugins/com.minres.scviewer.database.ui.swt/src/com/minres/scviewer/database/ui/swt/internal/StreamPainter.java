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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
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
	private TreeSet<ITx> seenTx;

	public StreamPainter(WaveformCanvas waveCanvas, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = waveCanvas;
		this.stream=trackEntry.waveform;
		this.seenTx=new TreeSet<>();
	}

	@SuppressWarnings("unchecked")
	public void paintArea(Projection proj, Rectangle area) {
		if(stream.getEvents().size()==0) return;
		int trackHeight=trackEntry.height/stream.getWidth();
		txBase=trackHeight/5;
		txHeight=trackHeight*3/5;
		if(trackEntry.selected) {
			proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_HIGHLITE));
		}
		else
			proj.setBackground(this.waveCanvas.styleProvider.getColor(even?WaveformColors.TRACK_BG_EVEN:WaveformColors.TRACK_BG_ODD));
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);

		long scaleFactor = this.waveCanvas.getScaleFactor();
		long beginPos = area.x;
		long beginTime = beginPos*scaleFactor;
		long endTime = beginTime + area.width*scaleFactor;

		Entry<Long, ?> firstTx=stream.getEvents().floorEntry(beginTime);
		Entry<Long, ?> lastTx=stream.getEvents().ceilingEntry(endTime);
		if(firstTx==null) firstTx = stream.getEvents().firstEntry();
		if(lastTx==null) lastTx=stream.getEvents().lastEntry();
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.setLineStyle(SWT.LINE_SOLID);
		proj.setLineWidth(1);
		proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE));

		for( int y1=area.y+trackHeight/2; y1<area.y+trackEntry.height; y1+=trackHeight)
			proj.drawLine(area.x, y1, area.x+area.width, y1);
		if(firstTx==lastTx) {
			for(ITxEvent txEvent:(Collection<?  extends ITxEvent>)firstTx.getValue())
				drawTx(proj, area, txEvent.getTransaction(), false);
		}else{
			seenTx.clear();
			NavigableMap<Long, IEvent[]> entries = stream.getEvents().subMap(firstTx.getKey(), true, lastTx.getKey(), true);
			boolean highlighed=false;
			proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE));

			for(Entry<Long, IEvent[]> entry: entries.entrySet())
				for(IEvent evt:entry.getValue()){
					ITxEvent txEvent = (ITxEvent) evt;
					if(txEvent.getKind()==EventKind.BEGIN)
						seenTx.add(txEvent.getTransaction());
					if(txEvent.getKind()==EventKind.END){
						ITx tx = txEvent.getTransaction();
						highlighed|=waveCanvas.currentSelection!=null && waveCanvas.currentSelection.equals(tx);
						drawTx(proj, area, tx, false);
						seenTx.remove(tx);
					}
				}
			for(ITx tx:seenTx){
				drawTx(proj, area, tx, false);
			}
			if(highlighed){
				proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE_HIGHLITE));
				drawTx(proj, area, waveCanvas.currentSelection, true);
			}
		}
	}

	protected void drawTx(Projection proj, Rectangle area, ITx tx, boolean highlighted ) {
		// compute colors
		Color[] transColor = waveCanvas.styleProvider.computeColor( tx.getGenerator().getName());

		proj.setBackground(transColor[highlighted?1:0]);

		int offset = tx.getConcurrencyIndex()*this.waveCanvas.styleProvider.getTrackHeight();
		Rectangle bb = new Rectangle(
				(int)(tx.getBeginTime()/this.waveCanvas.getScaleFactor()), area.y+offset+txBase,
				(int)((tx.getEndTime()-tx.getBeginTime())/this.waveCanvas.getScaleFactor()), txHeight);

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
		Entry<Long, IEvent[]> firstTx=stream.getEvents().floorEntry(point.x*waveCanvas.getScaleFactor());
		if(firstTx!=null){
			do {
				ITx tx = getTxFromEntry(lane, point.x, firstTx);
				if(tx!=null) return tx;
				firstTx=stream.getEvents().lowerEntry(firstTx.getKey());
			}while(firstTx!=null);
		}
		return null;
	}

	public IWaveform getStream() {
		return stream;
	}

	public void setStream(IWaveform stream) {
		this.stream = stream;
	}

	protected ITx getTxFromEntry(int lane, int offset, Entry<Long, IEvent[]> firstTx) {
		long timePoint=offset*waveCanvas.getScaleFactor();
		for(IEvent evt:firstTx.getValue()){
			if(evt instanceof ITxEvent) {
				ITx tx=((ITxEvent)evt).getTransaction();
				if(evt.getKind()==EventKind.BEGIN && tx.getConcurrencyIndex()==lane && tx.getBeginTime()<=timePoint && tx.getEndTime()>=timePoint){
					return ((ITxEvent)evt).getTransaction();
				}
			}
		}
		// now with some fuzziness
		timePoint=(offset-5)*waveCanvas.getScaleFactor();
		long timePointHigh=(offset+5)*waveCanvas.getScaleFactor();
		for(IEvent evt:firstTx.getValue()){
			if(evt instanceof ITxEvent) {
				ITx tx=((ITxEvent)evt).getTransaction();
				if(evt.getKind()==EventKind.BEGIN && tx.getConcurrencyIndex()==lane && tx.getBeginTime()<=timePointHigh && tx.getEndTime()>=timePoint){
					return ((ITxEvent)evt).getTransaction();
				}
			}
		}
		return null;
	}

}