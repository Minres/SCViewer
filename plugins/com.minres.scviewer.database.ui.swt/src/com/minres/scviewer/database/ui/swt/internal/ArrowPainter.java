/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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

	private List<LinkEntry> iRect = new LinkedList<>();

	private List<LinkEntry> oRect = new LinkedList<>();

	private Rectangle txRectangle;

	private RelationType highlightType;

	private long selectionOffset;
	
	long scaleFactor;

	public ArrowPainter(WaveformCanvas waveCanvas, RelationType relationType) {
		this.waveCanvas = waveCanvas;
		highlightType=relationType;
		updateSelection();
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

	public void updateSelection() {
		tx = waveCanvas.currentSelectedTx;
		scaleFactor = waveCanvas.getScale();
		if (tx != null) {
			calculateGeometries();
		}
	}

	private int getConcurrencyIndex(ITx tx) {
		IEvent[] eventList = waveCanvas.value(tx.getStream(), tx.getBeginTime());
		Optional<Integer> res = Arrays.stream(eventList).filter(e -> ((ITxEvent)e).getTransaction()== tx).map(e -> ((ITxEvent)e).getRowIndex()).findFirst();
		if(res.isPresent() && res.get()>0) {
			return res.get();
		} else 			
			return 0;
	}
	
	private Rectangle getBoundingBox(IWaveformPainter painter, ITx itx, IWaveform stream) {
		int laneHeight = painter.getHeight() / waveCanvas.rows(stream);
		//TODO: it needs to take the concurrency index within the current waveform (stream or generator) into account
		int concIdx=getConcurrencyIndex(itx);
		int laneOffset = laneHeight * (concIdx>=waveCanvas.rows(stream)?waveCanvas.rows(stream)-1:concIdx);
		int rulerHeight = waveCanvas.rulerHeight;
		Rectangle bb = new Rectangle((int) (itx.getBeginTime() / scaleFactor),
				rulerHeight + painter.getVerticalOffset() + laneOffset,
				(int) ((itx.getEndTime() - itx.getBeginTime()) / scaleFactor), laneHeight);
		return bb;
	}

	protected boolean calculateGeometries() {
		iRect.clear();
		oRect.clear();
		IWaveformPainter painter = null;
		if(waveCanvas.currentSelectedTrack != null)
			painter = waveCanvas.trackAreaPainter.getPainterFor(waveCanvas.currentSelectedTrack);
		if(painter == null)
			painter = waveCanvas.wave2painterMap.get(tx.getGenerator());
		if(painter==null)
			painter = waveCanvas.wave2painterMap.get(tx.getStream());
		if (painter == null) { // stream has been added but painter not yet created
			return true;
		}
		Rectangle bb = getBoundingBox(painter, tx, painter instanceof StreamPainter? ((StreamPainter)painter).getStream() : tx.getStream());
		txRectangle = bb;
		deriveGeom(waveCanvas.transactionData.get(tx).incoming(), iRect, false);
		deriveGeom(waveCanvas.transactionData.get(tx).outgoing(), oRect, true);
		return false;
	}

	protected void deriveGeom(Collection<ITxRelation> relations, List<LinkEntry> res, boolean useTarget) {
		for (ITxRelation iTxRelation : relations) {
			ITx otherTx = useTarget ? iTxRelation.getTarget() : iTxRelation.getSource();
			if (otherTx != null && otherTx.getBeginTime() >= 0)
				for (IWaveform iWaveform : new IWaveform[] { otherTx.getStream(), otherTx.getGenerator() }) {
					if (waveCanvas.wave2painterMap.containsKey(iWaveform)) {
						IWaveformPainter painter = waveCanvas.wave2painterMap.get(iWaveform);
						if (painter != null) {
							Rectangle bb = getBoundingBox(painter, otherTx,
									painter instanceof StreamPainter ? ((StreamPainter) painter).getStream()
											: otherTx.getStream());
							res.add(new LinkEntry(bb, iTxRelation.getRelationType()));
						}
					}
				}
		}
	}

	@Override
	public void paintArea(Projection proj, Rectangle clientRect) {
		yCtrlOffset = waveCanvas.styleProvider.getTrackHeight()/2;
		Color fgColor = waveCanvas.styleProvider.getColor(WaveformColors.REL_ARROW);
		Color highliteColor = waveCanvas.styleProvider.getColor(WaveformColors.REL_ARROW_HIGHLITE);

		if(tx==null) return;
		scaleFactor = waveCanvas.getScale();
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
		int arrowTipOffset=yCtrlOffset/2;

		if (point2.x > point1.x + srcRectangle.width)
			point1.x += srcRectangle.width;
		if (point1.x > point2.x + tgtRectangle.width)
			point2.x += tgtRectangle.width;

		Point endPoint = new Point(point2.x-arrowTipOffset, point2.y);
		Path path = new Path(Display.getCurrent());
		path.moveTo(point1.x, point1.y);
		if (point1.y == point2.y) {
			Point center = new Point((point1.x + endPoint.x) / 2, point1.y - yCtrlOffset);
			path.cubicTo(point1.x + X_CTRL_OFFSET, point1.y, center.x - X_CTRL_OFFSET, center.y, center.x, center.y);
			path.cubicTo(center.x + X_CTRL_OFFSET, center.y, endPoint.x - X_CTRL_OFFSET, endPoint.y, endPoint.x, endPoint.y);
		} else
			path.cubicTo(point1.x + X_CTRL_OFFSET, point1.y, endPoint.x - X_CTRL_OFFSET, endPoint.y, endPoint.x, endPoint.y);
		path.lineTo(point2.x, point2.y);
		proj.setAntialias(SWT.ON);
		proj.setForeground(fgColor);
		proj.getGC().drawPath(path);
		path.dispose();
		// now draw the arrow head
		Color background = proj.getGC().getBackground();
		proj.setBackground(fgColor);
		proj.getGC().fillPolygon(new int[] {
				point2.x, point2.y,
				point2.x - arrowTipOffset, point2.y - 3*arrowTipOffset/5,
				point2.x - 3*arrowTipOffset/5, point2.y,
				point2.x - arrowTipOffset, point2.y + 3*arrowTipOffset/5 });
		proj.setBackground(background);

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
