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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

import com.google.common.collect.Lists;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.ui.IWaveformStyleProvider;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.swt.Constants;

public class WaveformCanvas extends Canvas {
	
    private boolean doubleBuffering = true;
    	
    IWaveformStyleProvider styleProvider;
        
    private long scaleFactor = 1000000L; // 1ns
    
    String unit="ns";
    
    private int level = 12;
        
    private long maxTime;
    
    protected Point origin; /* original size */
        
    protected int rulerHeight=40;
    
    protected List<IPainter> painterList;
    
    ITx currentSelection;
    
    private List<SelectionAdapter> selectionListeners;

	private RulerPainter rulerPainter;

	private TrackAreaPainter trackAreaPainter;

	private ArrowPainter arrowPainter;

	private List<CursorPainter> cursorPainters;

	HashMap<IWaveform, IWaveformPainter> wave2painterMap;
    /**
     * Constructor for ScrollableCanvas.
     * 
     * @param parent
     *            the parent of this control.super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
     * @param style
     *            the style of this control.
     */
    public WaveformCanvas(final Composite parent, int style, IWaveformStyleProvider styleProvider) {
        super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.V_SCROLL | SWT.H_SCROLL);
        this.styleProvider=styleProvider;
    	addControlListener(new ControlAdapter() { /* resize listener. */
			@Override
			public void controlResized(ControlEvent event) {
                syncScrollBars();
            }
        });
        addPaintListener((final PaintEvent event) -> paint(event.gc));
        painterList = new LinkedList<>();
        origin = new Point(0, 0);
        selectionListeners = new LinkedList<>();
        cursorPainters= new ArrayList<>();
        wave2painterMap=new HashMap<>();
        
        initScrollBars();
		// order is important: it is bottom to top
        trackAreaPainter=new TrackAreaPainter(this);
        painterList.add(trackAreaPainter);
        arrowPainter=new ArrowPainter(this, IWaveformView.NEXT_PREV_IN_STREAM);
        painterList.add(arrowPainter);
        rulerPainter=new RulerPainter(this);
        painterList.add(rulerPainter);
		CursorPainter cp = new CursorPainter(this, scaleFactor * 10, cursorPainters.size()-1);
		painterList.add(cp);
		cursorPainters.add(cp);
		CursorPainter marker = new CursorPainter(this, scaleFactor * 100, cursorPainters.size()-1);
		painterList.add(marker);
		cursorPainters.add(marker);
		wave2painterMap=new HashMap<>();
    }
    
	public void addCursoPainter(CursorPainter cursorPainter){
		painterList.add(cursorPainter);
		cursorPainters.add(cursorPainter);
	}
	
    public void setHighliteRelation(RelationType relationType){
    	if(arrowPainter!=null){
    		boolean redraw = arrowPainter.getHighlightType()!=relationType; 
    		arrowPainter.setHighlightType(relationType);
    		if(redraw) redraw();
    	}
    }
    
    public Point getOrigin() {
        return origin;
    }

    public int getWidth() {
    	return getClientArea().width; 
    }
    public void setOrigin(Point origin) {
        setOrigin(origin.x, origin.y);
    }

    public void setOrigin(int x, int y) {
        checkWidget();
        ScrollBar hBar = getHorizontalBar();
        hBar.setSelection(-x);
        x = -hBar.getSelection();
        ScrollBar vBar = getVerticalBar();
        vBar.setSelection(-y);
        y = -vBar.getSelection();
        origin.x = x;
        origin.y = y;
        syncScrollBars();
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
        syncScrollBars();
    }

    public int getZoomLevel() {
        return level;
    }
    
    public int getMaxZoomLevel(){
    	return Constants.SCALE_MULTIPLIER.length*Constants.UNIT_STRING.length-1;
    }

    public void setZoomLevel(int level) {
		long tc=cursorPainters.get(0).getTime(); // cursor time
		setZoomLevel(level, tc);
    }

    public void setZoomLevel(int level, long centerTime) {
    	if(level<0) {
    		if(level<-1) {
    			long cTime = getCursorPainters().get(0).getTime();
    			long time_diff = centerTime>cTime?centerTime-cTime:cTime-centerTime;
    			level = findFitZoomLevel(time_diff);
    			centerTime = (centerTime>cTime?cTime:centerTime)+time_diff/2;
    		} else
    			level = findFitZoomLevel(maxTime);
	    	if(level<0) level = 0;
    	}
    	//FIXME: keep center if zoom-out and cursor is not in view
    	if(level<Constants.SCALE_MULTIPLIER.length*Constants.UNIT_STRING.length){
    		int scale = level%Constants.SCALE_MULTIPLIER.length;
    		int unit = level/Constants.SCALE_MULTIPLIER.length;
    		this.scaleFactor = Constants.UNIT_MULTIPLIER[unit]*Constants.SCALE_MULTIPLIER[scale];
    		ITx tx = arrowPainter.getTx();
    		arrowPainter.setTx(null);
    		/*
    		 * xc = tc/oldScaleFactor
    		 * xoffs = xc+origin.x
    		 * xcn = tc/newScaleFactor
    		 * t0n = (xcn-xoffs)*scaleFactor
    		 */
    		Rectangle clientArea = getClientArea();
    		long clientAreaWidth = clientArea.width;
    		long xoffs = clientAreaWidth/2;
			long xcn=centerTime/scaleFactor; // new total x-offset
			long originX=xcn-xoffs;
			if(originX>0) {
				origin.x=(int) -originX; // new cursor time offset relative to left border
			}else {
				origin.x=0;
			}
    		syncScrollBars();
    		arrowPainter.setTx(tx);    		
    		redraw();
    		this.level = level;
    	}
    }

	private int findFitZoomLevel(long duration) {
		//get area actually capable of displaying data, i.e. area of the receiver which is capable of displaying data
		Rectangle clientArea = getClientArea();
		long clientAreaWidth = clientArea.width;
		//try to find existing zoomlevel where scaleFactor*clientAreaWidth >= maxTime, if one is found set it as new zoomlevel
		for(int unitIdx=0; unitIdx<Constants.UNIT_STRING.length; unitIdx++) {
			long magnitudeMultiplier = Constants.UNIT_MULTIPLIER[unitIdx];
			for (int scaleIdx=0; scaleIdx<Constants.SCALE_MULTIPLIER.length; scaleIdx++){
				long scaleFactor = magnitudeMultiplier * Constants.SCALE_MULTIPLIER[scaleIdx];
				long range = scaleFactor*clientAreaWidth;
			    if( range >= duration)
			    	return unitIdx*Constants.SCALE_MULTIPLIER.length+scaleIdx;
			}
		}
		return -1;
	}

    public long getScaleFactor() {
        return scaleFactor;
    }

    public long getScaleFactorPow10() {
    	int scale = level/Constants.SCALE_MULTIPLIER.length;
    	double res = Math.pow(1000, scale);
    	return (long) res;
    }

    public String getUnitStr(){
        return Constants.UNIT_STRING[level/Constants.SCALE_MULTIPLIER.length];
    }
     
    public long getUnitMultiplier(){
        return Constants.SCALE_MULTIPLIER[level%Constants.SCALE_MULTIPLIER.length];
    }
    
    public long getTimeForOffset(int xOffset){
        return (xOffset-origin.x) * scaleFactor;
    }
    
    public void addPainter(IPainter painter) {
        painterList.add(painter);
        redraw();
    }

    public void removePainter(IPainter painter) {
        painterList.remove(painter);
        redraw();
    }

    public void clearAllWaveformPainter() {
    	clearAllWaveformPainter(true);
    }
    
    void clearAllWaveformPainter(boolean update) {
        trackAreaPainter.trackVerticalOffset.clear();
        wave2painterMap.clear();
        if(update) syncScrollBars();
    }

    public void addWaveformPainter(IWaveformPainter painter) {
    	addWaveformPainter(painter, true);
    }
    
    void addWaveformPainter(IWaveformPainter painter, boolean update) {
        trackAreaPainter.addTrackPainter(painter);
        wave2painterMap.put(painter.getTrackEntry().waveform, painter);
        if(update) syncScrollBars();
    }

    public List<CursorPainter> getCursorPainters() {
		return cursorPainters;
	}

    /* Initialize the scrollbar and register listeners. */
    private void initScrollBars() {
        ScrollBar horizontal = getHorizontalBar();
        horizontal.setEnabled(false);
        horizontal.setVisible(true);
        horizontal.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent event) {
                if (painterList.isEmpty())
                    return;
                setOrigin(-((ScrollBar) event.widget).getSelection(), origin.y);
            }
        });
        ScrollBar vertical = getVerticalBar();
        vertical.setEnabled(false);
        vertical.setVisible(true);
        vertical.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent event) {
                if (painterList.isEmpty())
                    return;
                setOrigin(origin.x, -((ScrollBar) event.widget).getSelection());
            }
        });
    }

    /**
     * Synchronize the scrollbar with the image. If the transform is out of
     * range, it will correct it. This function considers only following factors
     * :<b> transform, image size, client area</b>.
     */
    public void syncScrollBars() {
        if (painterList.isEmpty()) {
            redraw();
            return;
        }
        int height = trackAreaPainter.getHeight(); // incl. Ruler
        long width = maxTime / scaleFactor;
        Rectangle clientArea=getClientArea();
        ScrollBar horizontal = getHorizontalBar();
        horizontal.setIncrement(getClientArea().width / 100);
        horizontal.setPageIncrement(getClientArea().width);
        int clientWidthw = clientArea.width;
        if (width > clientWidthw) { /* image is wider than client area */
        	horizontal.setMinimum(0);
            horizontal.setMaximum((int)width);
            horizontal.setEnabled(true);
            if (-origin.x > horizontal.getMaximum() - clientWidthw) {
                origin.x = -horizontal.getMaximum() + clientWidthw;
            }
        } else { /* image is narrower than client area */
            horizontal.setEnabled(false);
        }
        horizontal.setThumb(clientWidthw);
        horizontal.setSelection(-origin.x);

        ScrollBar vertical = getVerticalBar();
        vertical.setIncrement(getClientArea().height / 100);
        vertical.setPageIncrement(getClientArea().height);
        int clientHeighth = clientArea.height;
        if (height > clientHeighth) { /* image is higher than client area */
            vertical.setMinimum(0);
            vertical.setMaximum(height);
            vertical.setEnabled(true);
            if ( -origin.y > vertical.getMaximum() - clientHeighth) {
                origin.y = -vertical.getMaximum() + clientHeighth;
            }
        } else { /* image is less higher than client area */
            vertical.setMaximum(clientHeighth);
            vertical.setEnabled(false);
        }
        vertical.setThumb(clientHeighth);
        vertical.setSelection(-origin.y);
        redraw();
        fireSelectionEvent();
    }

    /* Paint function */
    private void paint(GC gc) {
        Point pt = getSize();
        if(pt.x==0  || pt.y==0) return;
        Rectangle clientRect = getClientArea(); /* Canvas' painting area */
        GC thisGc = gc;
        Image dBackingImg = null;
        if(doubleBuffering) {
        	dBackingImg = new Image(getDisplay(), pt.x, pt.y);
            thisGc = new GC(dBackingImg);
            thisGc.setBackground(gc.getBackground());
            thisGc.setForeground(gc.getForeground());
            thisGc.setFont(gc.getFont());
            
        }
        Projection p = new Projection(thisGc);
        p.setTranslation(origin);
        if (!painterList.isEmpty() ) {
            for (IPainter painter : painterList)
                painter.paintArea(p, clientRect);
        } else {
            gc.fillRectangle(clientRect);
            initScrollBars();
        }
        if(doubleBuffering) {
            gc.drawImage(dBackingImg, 0, 0);
            if(dBackingImg!=null) dBackingImg.dispose();
            thisGc.dispose();
        }
    }

    public List<Object> getElementsAt(Point point) {
    	LinkedList<Object> result=new LinkedList<>();
        for (IPainter p : Lists.reverse(painterList)) {
            if (p instanceof TrackAreaPainter) {
                int y = point.y - origin.y;
                int x = point.x - origin.x;
                Entry<Integer, IWaveformPainter> entry = trackAreaPainter.trackVerticalOffset.floorEntry(y);
                if (entry != null) {
                    if (entry.getValue() instanceof StreamPainter) {
                    	ITx tx = ((StreamPainter) entry.getValue()).getClicked(new Point(x, y - entry.getKey()));
                    	if(tx!=null)
                    		result.add(tx);
                    } 
                    result.add(entry.getValue().getTrackEntry());
                }
            } else if (p instanceof CursorPainter) {
                if (Math.abs(point.x - origin.x - ((CursorPainter) p).getTime()/scaleFactor) < 2) {
                	result.add(p);
                }
            }
        }
        return result;
    }

    public List<Object> getEntriesAtPosition(IWaveform iWaveform, int i) {
    	LinkedList<Object> result=new LinkedList<>();
        int x = i - origin.x;
        for(IWaveformPainter p: wave2painterMap.values()){
        	if (p instanceof StreamPainter && ((StreamPainter)p).getStream()==iWaveform) {
        		result.add(((StreamPainter) p).getClicked(new Point(x, styleProvider.getTrackHeight()/2)));
        	}
        }
        return result;
    }

    public void setSelected(ITx currentSelection) {
        this.currentSelection = currentSelection;
        if (currentSelection != null)
            reveal(currentSelection);
        arrowPainter.setTx(currentSelection);
        redraw();
    }

    public void reveal(ITx tx) {
        int lower = (int) (tx.getBeginTime() / scaleFactor);
        int higher = (int) (tx.getEndTime() / scaleFactor);
        Point size = getSize();
        size.x -= getVerticalBar().getSize().x + 2;
        size.y -= getHorizontalBar().getSize().y;
        if (lower < -origin.x) {
            setOrigin(-lower, origin.y);
        } else if (higher > (size.x - origin.x)) {
            setOrigin(size.x - higher, origin.y);
        }
        for (IWaveformPainter painter : wave2painterMap.values()) {
            if (painter instanceof StreamPainter && ((StreamPainter) painter).getStream() == tx.getStream()) {
            	EventEntry entry = tx.getStream().getEvents().floorEntry(tx.getBeginTime());
            	Optional<IEvent> res = Arrays.stream(entry.events).filter(e -> ((ITxEvent)e).getTransaction().equals(tx)).findFirst();
            	if(res.isPresent()) {
                    int top = painter.getVerticalOffset() + styleProvider.getTrackHeight() * ((ITxEvent)res.get()).getRowIndex();
                    int bottom = top + styleProvider.getTrackHeight();
                    if (top < -origin.y) {
                        setOrigin(origin.x, -(top-styleProvider.getTrackHeight()));
                    } else if (bottom > (size.y - origin.y)) {
                        setOrigin(origin.x, size.y - bottom);
                    }
            	}
            }
        }
    }
    
	public void reveal(IWaveform waveform) {
        for (IWaveformPainter painter : wave2painterMap.values()) {
        	TrackEntry te = painter.getTrackEntry();
        	if(te.waveform == waveform) {
                Point size = getSize();
                size.y -=+rulerHeight;
                ScrollBar sb = getHorizontalBar();
                if((sb.getStyle()&SWT.SCROLLBAR_OVERLAY)!=0 && sb.isVisible())
                	size.y-=  getHorizontalBar().getSize().y;
                int top = te.vOffset;
                int bottom = top + styleProvider.getTrackHeight();
                if (top < -origin.y) {
                    setOrigin(origin.x, -(top-styleProvider.getTrackHeight()));
                } else if (bottom > (size.y - origin.y)) {
                    setOrigin(origin.x, size.y - bottom);
                }
            }
        }
	}

    public void reveal(long time) {
        int scaledTime = (int) (time / scaleFactor);
        Point size = getSize();
        size.x -= getVerticalBar().getSize().x + 2;
        size.y -= getHorizontalBar().getSize().y;
        if (scaledTime < -origin.x) {
            setOrigin(-scaledTime+10, origin.y);
        } else if (scaledTime > (size.x - origin.x)) {
            setOrigin(size.x - scaledTime-30, origin.y);
        }
    }

    public void centerAt(long time) {
        int scaledTime = (int) (time / scaleFactor);
        int newX = -scaledTime+getWidth()/2;
        setOrigin(newX>0?0:newX, origin.y);
    }

    public int getRulerHeight() {
        return rulerHeight;
    }

    public void setRulerHeight(int rulerHeight) {
        this.rulerHeight = rulerHeight;
    }

    public void addSelectionListener(SelectionAdapter selectionAdapter) {
        selectionListeners.add(selectionAdapter);
    }

    public void removeSelectionListener(SelectionAdapter selectionAdapter) {
        selectionListeners.remove(selectionAdapter);
    }

    /**
     * 
     */
    protected void fireSelectionEvent() {
        Event e = new Event();
        e.widget = this;
        e.detail=SWT.SELECTED;
        e.type=SWT.Selection;
        SelectionEvent ev = new SelectionEvent(e);
        ev.x = origin.x;
        ev.y = origin.y;
        for (SelectionAdapter a : selectionListeners) {
            a.widgetSelected(ev);
        }
    }

    long getMaxVisibleTime() {
    	return (getClientArea().width+origin.x)*scaleFactor;
    }

    long getMinVisibleTime() {
    	return origin.x * scaleFactor;
    }

	public void setStyleProvider(IWaveformStyleProvider styleProvider) {
		this.styleProvider=styleProvider;
		redraw();
	}
}
