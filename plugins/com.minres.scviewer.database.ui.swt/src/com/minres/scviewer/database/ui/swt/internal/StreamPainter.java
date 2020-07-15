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
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class StreamPainter extends TrackPainter{

	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;
	private ITxStream<? extends ITxEvent> stream;
	private int txBase, txHeight;
	private boolean even;
	private TreeSet<ITx> seenTx;

	public StreamPainter(WaveformCanvas waveCanvas, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = waveCanvas;
		this.stream=trackEntry.getStream();
		this.seenTx=new TreeSet<ITx>();
	}
    
	/*
	 * convert java.awt.Color to org.eclipse.swt.graphics.Color 
	 */
	static org.eclipse.swt.graphics.Color toSwtColor( GC gc, java.awt.Color awtColor ){
		return new org.eclipse.swt.graphics.Color( gc.getDevice(), awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue() );
	}

	static org.eclipse.swt.graphics.Color[] toSwtColors( GC gc, java.awt.Color[] awtColors ){
		org.eclipse.swt.graphics.Color[] swtColors = new org.eclipse.swt.graphics.Color[awtColors.length];
		for( int i=0; i<awtColors.length; i++ )
			swtColors[i] = toSwtColor( gc, awtColors[i] );
		return swtColors;
	}

	@SuppressWarnings("unchecked")
	public void paintArea(Projection proj, Rectangle area) {
		if(stream.getEvents().size()==0) return;
		int trackHeight=trackEntry.height/stream.getMaxConcurrency();
		txBase=trackHeight/5;
		txHeight=trackHeight*3/5;
		if(trackEntry.selected) {
			proj.setBackground(this.waveCanvas.colors[WaveformColors.TRACK_BG_HIGHLITE.ordinal()]);
		}
		else
			proj.setBackground(this.waveCanvas.colors[even?WaveformColors.TRACK_BG_EVEN.ordinal():WaveformColors.TRACK_BG_ODD.ordinal()]);
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);
		
		long scaleFactor = this.waveCanvas.getScaleFactor();
		long beginPos = area.x;
		long beginTime = beginPos*scaleFactor;
		//long endPos = beginPos + area.width;
        long endTime = beginTime + area.width*scaleFactor;

		Entry<Long, ?> firstTx=stream.getEvents().floorEntry(beginTime);
		Entry<Long, ?> lastTx=stream.getEvents().ceilingEntry(endTime);
		if(firstTx==null) firstTx = stream.getEvents().firstEntry();
		if(lastTx==null) lastTx=stream.getEvents().lastEntry();
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.setLineStyle(SWT.LINE_SOLID);
		proj.setLineWidth(1);
		proj.setForeground(this.waveCanvas.colors[WaveformColors.LINE.ordinal()]);
        
        for( int y1=area.y+trackHeight/2; y1<area.y+trackEntry.height; y1+=trackHeight)
        	proj.drawLine(area.x, y1, area.x+area.width, y1);
		if(firstTx==lastTx) {
			for(ITxEvent txEvent:(Collection<?  extends ITxEvent>)firstTx.getValue())
				drawTx(proj, area, txEvent.getTransaction(), false);
		}else{
			seenTx.clear();
			NavigableMap<Long,?> entries = stream.getEvents().subMap(firstTx.getKey(), true, lastTx.getKey(), true);
			boolean highlighed=false;
	        proj.setForeground(this.waveCanvas.colors[WaveformColors.LINE.ordinal()]);
	        
	        for(Entry<Long, ?> entry: entries.entrySet())
				for(ITxEvent txEvent:(Collection<?  extends ITxEvent>)entry.getValue()){
					if(txEvent.getType()==ITxEvent.Type.BEGIN)
						seenTx.add(txEvent.getTransaction());
					if(txEvent.getType()==ITxEvent.Type.END){
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
		        proj.setForeground(this.waveCanvas.colors[WaveformColors.LINE_HIGHLITE.ordinal()]);
		        drawTx(proj, area, waveCanvas.currentSelection, true);
			}
		}
	}
	
	protected void drawTx(Projection proj, Rectangle area, ITx tx, boolean highlighted ) {
		// compute colors
        java.awt.Color[] fallbackColors = trackEntry.getColors();
        java.awt.Color[] transColor = TrackEntry.computeColor( tx.getGenerator().getName(), fallbackColors[0], fallbackColors[1] );
        
        proj.setBackground( toSwtColor( proj.getGC(), transColor[highlighted?1:0] ) );
        
		int offset = tx.getConcurrencyIndex()*this.waveCanvas.getTrackHeight();
		Rectangle bb = new Rectangle(
				(int)(tx.getBeginTime()/this.waveCanvas.getScaleFactor()), area.y+offset+txBase,
				(int)((tx.getEndTime()-tx.getBeginTime())/this.waveCanvas.getScaleFactor()), txHeight);

		if(bb.x+bb.width<area.x || bb.x>area.x+area.width) return;
		if(bb.width==0){
			proj.drawLine(bb.x, bb.y, bb.x, bb.y+bb.height);
		} else if(bb.width<10){
			proj.fillRectangle(bb);
			proj.drawRectangle(bb);
		} else {
			if(bb.x < area.x) {
				bb.width = bb.width-(area.x-bb.x)+5;
				bb.x=area.x-5;
			}
			int bb_x2 = bb.x+bb.width;
			int area_x2 = area.x+area.width;
			if(bb_x2>area_x2){
				bb_x2=area_x2+5;
				bb.width= bb_x2-bb.x;
			}
			proj.fillRoundRectangle(bb.x, bb.y, bb.width, bb.height, 5, 5);
			proj.drawRoundRectangle(bb.x, bb.y, bb.width, bb.height, 5, 5);
		}
	}

	public ITx getClicked(Point point) {
		int lane=point.y/waveCanvas.getTrackHeight();
		Entry<Long, List<ITxEvent>> firstTx=stream.getEvents().floorEntry(point.x*waveCanvas.getScaleFactor());
		if(firstTx!=null){
			do {
				ITx tx = getTxFromEntry(lane, point.x, firstTx);
				if(tx!=null) return tx;
				firstTx=stream.getEvents().lowerEntry(firstTx.getKey());
			}while(firstTx!=null);
		}
		return null;
	}

	public ITxStream<? extends ITxEvent> getStream() {
		return stream;
	}

	public void setStream(ITxStream<? extends ITxEvent> stream) {
		this.stream = stream;
	}

	protected ITx getTxFromEntry(int lane, int offset, Entry<Long, List<ITxEvent>> firstTx) {
        long timePoint=offset*waveCanvas.getScaleFactor();
		for(ITxEvent evt:firstTx.getValue()){
		    ITx tx=evt.getTransaction();
			if(evt.getType()==ITxEvent.Type.BEGIN && tx.getConcurrencyIndex()==lane && tx.getBeginTime()<=timePoint && tx.getEndTime()>=timePoint){
				return evt.getTransaction();
			}
		}
		// now with some fuzziness
        timePoint=(offset-5)*waveCanvas.getScaleFactor();
        long timePointHigh=(offset+5)*waveCanvas.getScaleFactor();
        for(ITxEvent evt:firstTx.getValue()){
            ITx tx=evt.getTransaction();
            if(evt.getType()==ITxEvent.Type.BEGIN && tx.getConcurrencyIndex()==lane && tx.getBeginTime()<=timePointHigh && tx.getEndTime()>=timePoint){
                return evt.getTransaction();
            }
        }
		return null;
	}

}