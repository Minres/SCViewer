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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxRelation;
import com.minres.scviewer.database.ui.WaveformColors;

public class ArrowPainter implements IPainter {
	
	private static final float X_CTRL_OFFSET = 50;

	private int yCtrlOffset = 30;

	private final WaveformCanvas waveCanvas;

	private ITx tx;

	private List<LinkEntry> iRect;

	private List<LinkEntry> oRect;

	private Rectangle txRectangle;

	private RelationType highlightType;

	private long selectionOffset;
	
	long scaleFactor;

	public ArrowPainter(WaveformCanvas waveCanvas, RelationType relationType) {
		this.waveCanvas = waveCanvas;
		highlightType=relationType;
		setTx(null);
	}

	public RelationType getHighlightType() {
		return highlightType;
	}

	public void setHighlightType(RelationType highlightType) {
		this.highlightType = highlightType;
	}

	public ITx getTx() {
		return tx;
	}

	public void setTx(ITx newTx) {
		this.tx = newTx;
		iRect = new LinkedList<>();
		oRect = new LinkedList<>();
		scaleFactor = waveCanvas.getScaleFactor();
		if (tx != null) {
			calculateGeometries();
		}
	}

	private int getConcurrencyIndex(ITx tx) {
		IEvent[] eventList = tx.getStream().getEvents().floorEntry(tx.getBeginTime()).getValue();
		Optional<Integer> res = Arrays.stream(eventList).map(e -> ((ITxEvent)e).getRowIndex()).findFirst();
		return res.isPresent()? res.get():0;
	}
	
	protected boolean calculateGeometries() {
		iRect.clear();
		oRect.clear();
		IWaveformPainter painter = waveCanvas.wave2painterMap.get(tx.getStream());
		if(painter == null)
			painter = waveCanvas.wave2painterMap.get(tx.getGenerator());
		if (painter == null) { // stream has been added but painter not yet
								// created
			return true;
		}
		int laneHeight = painter.getHeight() / tx.getStream().getRowCount();
		txRectangle = new Rectangle((int) (tx.getBeginTime() / scaleFactor),
				waveCanvas.rulerHeight + painter.getVerticalOffset() + laneHeight * getConcurrencyIndex(tx),
				(int) ((tx.getEndTime() - tx.getBeginTime()) / scaleFactor), laneHeight);
		deriveGeom(tx.getIncomingRelations(), iRect, false);
		deriveGeom(tx.getOutgoingRelations(), oRect, true);
		return false;
	}

	protected void deriveGeom(Collection<ITxRelation> relations, List<LinkEntry> res, boolean useTarget) {
		for (ITxRelation iTxRelation : relations) {
			ITx otherTx = useTarget ? iTxRelation.getTarget() : iTxRelation.getSource();
			Rectangle bb = createLinkEntry(otherTx, otherTx.getStream());
			if(bb!=null){
				res.add(new LinkEntry(bb, iTxRelation.getRelationType()));
				return;
			} else {
				for(IHierNode gen:otherTx.getStream().getChildNodes()) {
					if(gen instanceof IWaveform) {
						bb = createLinkEntry(otherTx, (IWaveform) gen);
						if(bb!=null){
							res.add(new LinkEntry(bb, iTxRelation.getRelationType()));
							return;
						}
					}
				}
			}
		}
	}

	private Rectangle createLinkEntry(ITx otherTx, IWaveform iWaveform) {
		if (waveCanvas.wave2painterMap.containsKey(iWaveform)) {
			IWaveformPainter painter = waveCanvas.wave2painterMap.get(otherTx.getStream());
			if(painter==null) {
				for(IHierNode gen:otherTx.getStream().getChildNodes()) {
					if(gen instanceof IWaveform) {
						 painter = waveCanvas.wave2painterMap.get(gen);
						 if(painter!=null)
							 break;
					}
				}
			}
			if(painter!=null) {
				int height = waveCanvas.styleProvider.getTrackHeight();
				return new Rectangle(
						(int) (otherTx.getBeginTime() / scaleFactor),
						waveCanvas.rulerHeight + painter.getVerticalOffset() + height * getConcurrencyIndex(otherTx),
						(int) ((otherTx.getEndTime() - otherTx.getBeginTime()) / scaleFactor),
						height);
			}
		}
		return null;
	}

	@Override
	public void paintArea(Projection proj, Rectangle clientRect) {
		yCtrlOffset = waveCanvas.styleProvider.getTrackHeight()/2;
		Color fgColor = waveCanvas.styleProvider.getColor(WaveformColors.REL_ARROW);
		Color highliteColor = waveCanvas.styleProvider.getColor(WaveformColors.REL_ARROW_HIGHLITE);

		if(tx==null) return;
		scaleFactor = waveCanvas.getScaleFactor();
		if(calculateGeometries())
			return;
		int correctionValue = (int)(selectionOffset);
		Rectangle correctedTargetRectangle = new Rectangle(txRectangle.x+correctionValue, txRectangle.y, txRectangle.width, txRectangle.height);
		for (LinkEntry entry : iRect) {
			Rectangle correctedRectangle = new Rectangle(entry.rectangle.x+correctionValue, entry.rectangle.y, entry.rectangle.width, entry.rectangle.height);
			drawArrow(proj, highlightType.equals(entry.relationType) ? highliteColor : fgColor,
					correctedRectangle, correctedTargetRectangle);
		}
		for (LinkEntry entry : oRect) {
			Rectangle correctedRectangle = new Rectangle(entry.rectangle.x+correctionValue, entry.rectangle.y, entry.rectangle.width, entry.rectangle.height);
			drawArrow(proj, highlightType.equals(entry.relationType) ? highliteColor : fgColor, correctedTargetRectangle,
					correctedRectangle);
		}
	}

	protected void drawArrow(Projection proj, Color fgColor, Rectangle srcRectangle, Rectangle tgtRectangle) {
		Point point1 = proj.project(new Point(srcRectangle.x, srcRectangle.y + srcRectangle.height / 2));
		Point point2 = proj.project(new Point(tgtRectangle.x, tgtRectangle.y + tgtRectangle.height / 2));

		if (point2.x > point1.x + srcRectangle.width)
			point1.x += srcRectangle.width;
		if (point1.x > point2.x + tgtRectangle.width)
			point2.x += tgtRectangle.width;

		Path path = new Path(Display.getCurrent());
		path.moveTo(point1.x, point1.y);
		if (point1.y == point2.y) {
			Point center = new Point((point1.x + point2.x) / 2, point1.y - yCtrlOffset);
			path.cubicTo(point1.x + X_CTRL_OFFSET, point1.y, center.x - X_CTRL_OFFSET, center.y, center.x, center.y);
			path.cubicTo(center.x + X_CTRL_OFFSET, center.y, point2.x - X_CTRL_OFFSET, point2.y, point2.x, point2.y);
		} else
			path.cubicTo(point1.x + X_CTRL_OFFSET, point1.y, point2.x - X_CTRL_OFFSET, point2.y, point2.x, point2.y);

		proj.setAntialias(SWT.ON);
		proj.setForeground(fgColor);
		proj.getGC().drawPath(path);
		path.dispose();
		// now draw the arrow head
		proj.getGC().drawLine(point2.x - 8, point2.y - 5, point2.x, point2.y);
		proj.getGC().drawLine(point2.x - 8, point2.y + 5, point2.x, point2.y);

	}

	class LinkEntry {
		public final Rectangle rectangle;
		public final RelationType relationType;

		public LinkEntry(Rectangle rectangle, RelationType relationType) {
			super();
			this.rectangle = rectangle;
			this.relationType = relationType;
		}
	}

}
