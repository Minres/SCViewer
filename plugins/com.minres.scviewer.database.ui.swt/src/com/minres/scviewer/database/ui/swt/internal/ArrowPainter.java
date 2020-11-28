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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;
import com.minres.scviewer.database.ui.WaveformColors;

public class ArrowPainter implements IPainter {
	
	private final int xCtrlOffset = 50;

	private final int yCtrlOffset = 30;

	private WaveformCanvas waveCanvas;

	private ITx tx;

	private List<LinkEntry> iRect;

	private List<LinkEntry> oRect;

	private Rectangle txRectangle;

	private RelationType highlightType;

	private long selectionOffset;
	
	long scaleFactor;

	boolean deferUpdate;

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

	protected void calculateGeometries() {
		deferUpdate = false;
		iRect.clear();
		oRect.clear();
		IWaveformPainter painter = waveCanvas.wave2painterMap.get(tx.getStream());
		if (painter == null) { // stream has been added but painter not yet
								// created
			deferUpdate = true;
			return;
		}
		int laneHeight = painter.getHeight() / tx.getStream().getWidth();
		txRectangle = new Rectangle((int) (tx.getBeginTime() / scaleFactor),
				waveCanvas.rulerHeight + painter.getVerticalOffset() + laneHeight * tx.getConcurrencyIndex(),
				(int) ((tx.getEndTime() - tx.getBeginTime()) / scaleFactor), laneHeight);
		deriveGeom(tx.getIncomingRelations(), iRect, false);
		deriveGeom(tx.getOutgoingRelations(), oRect, true);
	}

	protected void deriveGeom(Collection<ITxRelation> relations, List<LinkEntry> res, boolean useTarget) {
		for (ITxRelation iTxRelation : relations) {
			ITx otherTx = useTarget ? iTxRelation.getTarget() : iTxRelation.getSource();
			if (waveCanvas.wave2painterMap.containsKey(otherTx.getStream())) {
				IWaveformPainter painter = waveCanvas.wave2painterMap.get(otherTx.getStream());
				int laneHeight = painter.getHeight() / tx.getStream().getWidth();
				Rectangle bb = new Rectangle((int) (otherTx.getBeginTime() / scaleFactor),
						waveCanvas.rulerHeight + painter.getVerticalOffset()
								+ laneHeight * otherTx.getConcurrencyIndex(),
						(int) ((otherTx.getEndTime() - otherTx.getBeginTime()) / scaleFactor), laneHeight);
				res.add(new LinkEntry(bb, iTxRelation.getRelationType()));
			}
		}
	}

	@Override
	public void paintArea(Projection proj, Rectangle clientRect) {
		Color fgColor = waveCanvas.colors[WaveformColors.REL_ARROW.ordinal()];
		Color highliteColor = waveCanvas.colors[WaveformColors.REL_ARROW_HIGHLITE.ordinal()];

		if(tx==null) return;
		if (!deferUpdate) {
			scaleFactor = waveCanvas.getScaleFactor();
			calculateGeometries();
		}
		if(deferUpdate) return;
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
			path.cubicTo(point1.x + xCtrlOffset, point1.y, center.x - xCtrlOffset, center.y, center.x, center.y);
			path.cubicTo(center.x + xCtrlOffset, center.y, point2.x - xCtrlOffset, point2.y, point2.x, point2.y);
		} else
			path.cubicTo(point1.x + xCtrlOffset, point1.y, point2.x - xCtrlOffset, point2.y, point2.x, point2.y);

		proj.setAntialias(SWT.ON);
		proj.setForeground(fgColor);
		proj.getGC().drawPath(path);
		path.dispose();
		// now draw the arrow head
		proj.getGC().drawLine(point2.x - 8, point2.y - 5, point2.x, point2.y);
		proj.getGC().drawLine(point2.x - 8, point2.y + 5, point2.x, point2.y);

	}

	class LinkEntry {
		public Rectangle rectangle;
		public RelationType relationType;

		public LinkEntry(Rectangle rectangle, RelationType relationType) {
			super();
			this.rectangle = rectangle;
			this.relationType = relationType;
		}
	}

}
