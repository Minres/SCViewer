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
package com.minres.scviewer.database.swt.internal;

import java.util.Map.Entry;
import java.util.NavigableMap;

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ISignalChange;
import com.minres.scviewer.database.ISignalChangeMulti;
import com.minres.scviewer.database.ISignalChangeSingle;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class SignalPainter extends TrackPainter {
	private class SignalChange {
		long time;
		ISignalChange value;
		boolean fromMap;

		public SignalChange(Entry<Long, ? extends ISignalChange> entry) {
			time = entry.getKey();
			value = entry.getValue();
			fromMap = true;
		}

		public void set(Entry<Long, ? extends ISignalChange> entry, Long actTime) {
			if (entry != null) {
				time = entry.getKey();
				value = entry.getValue();
				fromMap = true;
			} else {
				time = actTime;
				fromMap = false;
			}
		}

		public void assign(SignalChange other) {
			time = other.time;
			value = other.value;
			fromMap = other.fromMap;
		}
	}

	/**
	 * 
	 */
	private static final JPanel DUMMY_PANEL = new JPanel();

	private final WaveformCanvas waveCanvas;
	private ISignal<? extends ISignalChange> signal;

	int yOffsetT;
	int yOffsetM;
	int yOffsetB;
	int maxX;

	public SignalPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
		this.signal = trackEntry.getSignal();
	}

	private int getXEnd(long time) {
		long ltmp = time / this.waveCanvas.getScaleFactor();
		return ltmp > maxX ? maxX : (int) ltmp;
	}

	public void paintArea(GC gc, Rectangle area) {
		if (trackEntry.selected)
			gc.setBackground(this.waveCanvas.colors[WaveformColors.TRACK_BG_HIGHLITE.ordinal()]);
		else
			gc.setBackground(this.waveCanvas.colors[even ? WaveformColors.TRACK_BG_EVEN.ordinal() : WaveformColors.TRACK_BG_ODD.ordinal()]);
		gc.setFillRule(SWT.FILL_EVEN_ODD);
		gc.fillRectangle(area);
		long beginTime = area.x * this.waveCanvas.getScaleFactor();
		long endTime = (area.x + area.width) * this.waveCanvas.getScaleFactor();
		Entry<Long, ? extends ISignalChange> first = signal.getEvents().floorEntry(beginTime);
		Entry<Long, ? extends ISignalChange> last = signal.getEvents().floorEntry(endTime);
		if (first == null) {
			if (last == null)
				return;
			first = signal.getEvents().firstEntry();
		} else if (last == null) {
			last = signal.getEvents().lastEntry();
		}
		gc.setForeground(this.waveCanvas.colors[WaveformColors.LINE.ordinal()]);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		NavigableMap<Long, ? extends ISignalChange> entries = signal.getEvents().subMap(first.getKey(), false, last.getKey(), true);
		SignalChange left = new SignalChange(first);
		SignalChange right = new SignalChange(entries.size() > 0 ? entries.firstEntry() : first);
		SignalStencil stencil = left.value instanceof ISignalChangeSingle ? new SingleBitStencil() : new MultiBitStencil(gc);

		maxX = area.x + area.width;
		yOffsetT = this.waveCanvas.getTrackHeight() / 5 + area.y;
		yOffsetM = this.waveCanvas.getTrackHeight() / 2 + area.y;
		yOffsetB = 4 * this.waveCanvas.getTrackHeight() / 5 + area.y;
		int xBegin = Math.max(area.x, (int) (left.time / this.waveCanvas.getScaleFactor()));
		int xEnd = Math.max(area.x, getXEnd(right.time));
		boolean multiple = false;
		if (xEnd == xBegin) {
			// this can trigger if
			// a) left == right
			// b) left to close to right
			if (left.time == right.time) {
				right.time = endTime;

			} else {
				multiple = true;
				long eTime = (xBegin + 1) * this.waveCanvas.getScaleFactor();
				right.set(entries.floorEntry(eTime), endTime);
				right.time = eTime;
			}
			xEnd = getXEnd(right.time);
		}

		do {
			stencil.draw(gc, area, left.value, right.value, xBegin, xEnd, multiple);
			if (right.time >= endTime)
				break;
			left.assign(right);
			xBegin = xEnd;
			right.set(entries.higherEntry(left.time), endTime);
			xEnd = getXEnd(right.time);
			multiple = false;
			if (xEnd == xBegin) {
				multiple = true;
				long eTime = (xBegin + 1) * this.waveCanvas.getScaleFactor();
				right.set(entries.floorEntry(eTime), endTime);
				xEnd = getXEnd(eTime);
			}
		} while (left.time < endTime);
	}

	private interface SignalStencil {

		public void draw(GC gc, Rectangle area, ISignalChange left, ISignalChange right, int xBegin, int xEnd, boolean multiple);
	}

	private class MultiBitStencil implements SignalStencil {

		private java.awt.Font tmpAwtFont;
		private int height;

		public MultiBitStencil(GC gc) {
			FontData fd = gc.getFont().getFontData()[0];
			height = gc.getDevice().getDPI().y * fd.getHeight() / 72;
			tmpAwtFont = new java.awt.Font(fd.getName(), fd.getStyle(), height);
		}

		public void draw(GC gc, Rectangle area, ISignalChange left, ISignalChange right, int xBegin, int xEnd, boolean multiple) {
			Color colorBorder = waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
			ISignalChangeMulti last = (ISignalChangeMulti) left;
			if (last.getValue().toString().contains("X")) {
				colorBorder = waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
			} else if (last.getValue().toString().contains("Z")) {
				colorBorder = waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
			}
			int width = xEnd - xBegin;
			if (width > 1) {
				int[] points = { 
						xBegin,     yOffsetM, 
						xBegin + 1, yOffsetT, 
						xEnd - 1,   yOffsetT, 
						xEnd,       yOffsetM, 
						xEnd - 1,   yOffsetB, 
						xBegin + 1, yOffsetB
				};
				gc.setForeground(colorBorder);
				gc.drawPolygon(points);
				gc.setForeground(waveCanvas.colors[WaveformColors.SIGNAL_TEXT.ordinal()]);
				String label = "h'" + last.getValue().toHexString();
				Point bb = getBoxWidth(gc, label);
				if (xBegin < area.x) {
					xBegin = area.x;
					width = xEnd - xBegin;
				}
				if (width > (bb.x+1)) {
					Rectangle old = gc.getClipping();
					gc.setClipping(xBegin + 3, yOffsetT, xEnd - xBegin - 5, yOffsetB - yOffsetT);
					gc.drawText(label, xBegin + 3, yOffsetM - bb.y / 2 - 1);
					gc.setClipping(old);
				}
			} else {
				gc.setForeground(colorBorder);
				gc.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
			}
		}

		private Point getBoxWidth(GC gc, String label) {
			return new Point(DUMMY_PANEL.getFontMetrics(tmpAwtFont).stringWidth(label), height);
		}

	}

	private class SingleBitStencil implements SignalStencil {
		public void draw(GC gc, Rectangle area, ISignalChange left, ISignalChange right, int xBegin, int xEnd, boolean multiple) {
			if (multiple) {
				gc.setForeground(waveCanvas.colors[WaveformColors.SIGNALU.ordinal()]);
				gc.drawLine(xBegin, yOffsetT, xBegin, yOffsetB);
				gc.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
			} else {
				Color color = waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
				int yOffset = yOffsetM;
				switch (((ISignalChangeSingle) left).getValue()) {
				case '1':
					color = waveCanvas.colors[WaveformColors.SIGNAL1.ordinal()];
					yOffset = yOffsetT;
					break;
				case '0':
					color = waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
					yOffset = yOffsetB;
					break;
				case 'Z':
					color = waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
					break;
				default:
				}
				gc.setForeground(color);
				if (xEnd > maxX) {
					gc.drawLine(xBegin, yOffset, maxX, yOffset);
				} else {
					gc.drawLine(xBegin, yOffset, xEnd, yOffset);
					int yNext = yOffsetM;
					switch (((ISignalChangeSingle) right).getValue()) {
					case '1':
						yNext = yOffsetT;
						break;
					case '0':
						yNext = yOffsetB;
						break;
					default:
					}
					if (yOffset != yNext)
						gc.drawLine(xEnd, yOffset, xEnd, yNext);
				}
			}
		}

	}

	public ISignal<? extends ISignalChange> getSignal() {
		return signal;
	}

}