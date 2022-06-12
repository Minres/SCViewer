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

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DoubleVal;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class SignalPainter extends TrackPainter {
	private class SignalChange {
		long time;
		IEvent value;
		boolean fromMap;

		public SignalChange(EventEntry entry) {
			time = entry.timestamp;
			value = entry.events[0];
			fromMap = true;
		}

		public void set(EventEntry entry, Long actTime) {
			if (entry != null) {
				time = entry.timestamp;
				value = entry.events[0];
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

	int yOffsetT;
	int yOffsetM;
	int yOffsetB;
	/// maximum visible canvas position in canvas coordinates
	int maxPosX;
	/// maximum visible position in waveform coordinates
	int maxValX;

	public SignalPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
	}

	private int getXPosEnd(long time) {
		long ltmp = time / this.waveCanvas.getScale();
		return ltmp > maxPosX ? maxPosX : (int) ltmp;
	}
	
	public void paintArea(Projection proj, Rectangle area) {
		IWaveform signal = trackEntry.waveform;
		if (trackEntry.selected)
			proj.setBackground(this.waveCanvas.styleProvider.getColor(WaveformColors.TRACK_BG_HIGHLITE));
		else
			proj.setBackground(this.waveCanvas.styleProvider.getColor(even ? WaveformColors.TRACK_BG_EVEN : WaveformColors.TRACK_BG_ODD));
		proj.setFillRule(SWT.FILL_EVEN_ODD);
		proj.fillRectangle(area);

		long scaleFactor = this.waveCanvas.getScale();
		long beginPos = area.x;
		long beginTime = beginPos*scaleFactor;
        long endTime = beginTime + area.width*scaleFactor;
		
		EventEntry first = signal.getEvents().floorEntry(beginTime);
		if (first == null)
			first = signal.getEvents().firstEntry();
		beginTime = first.timestamp;
		proj.setForeground(this.waveCanvas.styleProvider.getColor(WaveformColors.LINE));
		proj.setLineStyle(SWT.LINE_SOLID);
		proj.setLineWidth(1);
		IEventList entries = signal.getEvents().subMap(beginTime, true, endTime);
		SignalChange left = new SignalChange(entries.firstEntry());
		SignalChange right = new SignalChange(entries.size() > 1 ? entries.higherEntry(left.time) : entries.firstEntry());
		maxPosX = area.x + area.width;
		yOffsetT = this.waveCanvas.styleProvider.getTrackHeight() / 5 + area.y;
		yOffsetM = this.waveCanvas.styleProvider.getTrackHeight() / 2 + area.y;
		yOffsetB = 4 * this.waveCanvas.styleProvider.getTrackHeight() / 5 + area.y;
		int xSigChangeBeginVal = Math.max(area.x, (int) (left.time / this.waveCanvas.getScale()));
		int xSigChangeBeginPos = area.x;
		int xSigChangeEndPos = Math.max(area.x, getXPosEnd(right.time));
		
		boolean multiple = false;
		if (xSigChangeEndPos == xSigChangeBeginPos) {
			// this can trigger if
			// a) left == right
			// b) left to close to right
			if (left.time == right.time) {
				right.time = endTime;
			} else {
				multiple = true;
				long eTime = (xSigChangeBeginVal + 1) * this.waveCanvas.getScale();
				right.set(entries.floorEntry(eTime), endTime);
				right.time = eTime;
			}
			xSigChangeEndPos = getXPosEnd(right.time);
		}

		
		SignalStencil stencil = getStencil(proj.getGC(), left, entries);
		if(stencil!=null) do {
			stencil.draw(proj, area, left.value, right.value, xSigChangeBeginPos, xSigChangeEndPos, multiple);
			if (right.time >= endTime)
				break;
			left.assign(right);
			xSigChangeBeginPos = xSigChangeEndPos;
			right.set(entries.higherEntry(left.time), endTime);
			xSigChangeEndPos = getXPosEnd(right.time);
			multiple = false;
			if (xSigChangeEndPos == xSigChangeBeginPos) {
				multiple = true;
				long eTime = (xSigChangeBeginPos + 1) * this.waveCanvas.getScale();
				EventEntry entry = entries.floorEntry(eTime);
				if(entry!=null && entry.timestamp> right.time)
					right.set(entry, endTime);
				xSigChangeEndPos = getXPosEnd(eTime);
			}
		} while (left.time < endTime);
	}

	private SignalStencil getStencil(GC gc, SignalChange left, IEventList entries) {
		IEvent val = left.value;
		if(val instanceof BitVector) {
			BitVector bv = (BitVector) val;
			if(bv.getWidth()==1)
				return new SingleBitStencil();				
			if(trackEntry.waveDisplay==TrackEntry.WaveDisplay.DEFAULT)
				return new MultiBitStencil(gc);
			else
				return new MultiBitStencilAnalog(entries, left.value, 
						trackEntry.waveDisplay==TrackEntry.WaveDisplay.CONTINOUS,
						trackEntry.valueDisplay==TrackEntry.ValueDisplay.SIGNED);
		} else if (val instanceof DoubleVal)
			return new RealStencil(entries, left.value, trackEntry.waveDisplay==TrackEntry.WaveDisplay.CONTINOUS);
		else
			return null;
	}

	private interface SignalStencil {

		public void draw(Projection proj, Rectangle area, IEvent left, IEvent right, int xBegin, int xEnd, boolean multiple);
	}

	private class MultiBitStencil implements SignalStencil {

		private java.awt.Font tmpAwtFont;
		private int height;

		public MultiBitStencil(GC gc) {
			FontData fd = gc.getFont().getFontData()[0];
			height = gc.getDevice().getDPI().y * fd.getHeight() / 72;
			tmpAwtFont = new java.awt.Font(fd.getName(), fd.getStyle(), height);
		}

		public void draw(Projection proj, Rectangle area, IEvent left, IEvent right, int xBegin, int xEnd, boolean multiple) {
			Color colorBorder = waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL0);
			BitVector last = (BitVector) left;
			if (Arrays.toString(last.getValue()).contains("X")) {
				colorBorder = waveCanvas.styleProvider.getColor(WaveformColors.SIGNALX);
			} else if (Arrays.toString(last.getValue()).contains("Z")) {
				colorBorder = waveCanvas.styleProvider.getColor(WaveformColors.SIGNALZ);
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
				proj.setForeground(colorBorder);
				proj.drawPolygon(points);
				proj.setForeground(waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL_TEXT));
				String label = null;
				switch(trackEntry.valueDisplay) {
				case SIGNED:
					label=Long.toString(last.toSignedValue());
					break;
				case UNSIGNED:
					label=Long.toString(last.toUnsignedValue());
					break;
				default:
					label="h'"+last.toHexString();
				}
				Point bb = new Point(DUMMY_PANEL.getFontMetrics(tmpAwtFont).stringWidth(label), height);
				if (xBegin < area.x) {
					xBegin = area.x;
					width = xEnd - xBegin;
				}
				if (width > (bb.x+1)) {
					Rectangle old = proj.getClipping();
					proj.setClipping(xBegin + 3, yOffsetT, xEnd - xBegin - 5, yOffsetB - yOffsetT);
					proj.drawText(label, xBegin + 3, yOffsetM - bb.y / 2 - 1);
					proj.setClipping(old);
				}
			} else {
				proj.setForeground(colorBorder);
				proj.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
			}
		}

	}

	private class MultiBitStencilAnalog implements SignalStencil {

		final boolean continous;
		final boolean signed;
		private long maxVal;
		private long minVal;
		double yRange = (yOffsetB-yOffsetT);
		public MultiBitStencilAnalog(IEventList entries, Object left, boolean continous, boolean signed) {
			this.continous=continous;
			this.signed=signed;
			Collection<EventEntry> ievents = entries.entrySet();
			minVal=signed?((BitVector)left).toSignedValue():((BitVector)left).toUnsignedValue();
			if(!ievents.isEmpty()) {
				maxVal=minVal;
				for (EventEntry tp : ievents)
					for(IEvent e: tp.events) {
						long v = signed?((BitVector)e).toSignedValue():((BitVector)e).toUnsignedValue();
						maxVal=Math.max(maxVal, v);
						minVal=Math.min(minVal, v);
					}
				if(maxVal==minVal) {
					maxVal--;
					minVal++;
				}
			} else {
				minVal--;
				maxVal=minVal+2;
			}
			
		}

		public void draw(Projection proj, Rectangle area, IEvent left, IEvent right, int xBegin, int xEnd, boolean multiple) {
			long leftVal = signed?((BitVector)left).toSignedValue():((BitVector)left).toUnsignedValue();
			long rightVal= signed?((BitVector)right).toSignedValue():((BitVector)right).toUnsignedValue();
			proj.setForeground(waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL_REAL));
			long range = maxVal-minVal;
			int yOffsetLeft = (int) ((leftVal-minVal) * yRange / range);
			int yOffsetRight = (int) ((rightVal-minVal) * yRange / range);
			if(continous) {
				if (xEnd > maxPosX) {
					proj.drawLine(xBegin, yOffsetB-yOffsetLeft, maxPosX, yOffsetB-yOffsetRight);
				} else {
					proj.drawLine(xBegin, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetRight);
				}
			} else {
				if (xEnd > maxPosX) {
					proj.drawLine(xBegin, yOffsetB-yOffsetLeft, maxPosX, yOffsetB-yOffsetLeft);
				} else {
					proj.drawLine(xBegin, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetLeft);
					if(yOffsetRight!=yOffsetLeft) {
						proj.drawLine(xEnd, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetRight);
					}
				}
			}
		}
	}

	private class SingleBitStencil implements SignalStencil {
		public void draw(Projection proj, Rectangle area, IEvent left, IEvent right, int xBegin, int xEnd, boolean multiple) {
			if (multiple) {
				proj.setForeground(waveCanvas.styleProvider.getColor(WaveformColors.SIGNALU));
				proj.drawLine(xBegin, yOffsetT, xBegin, yOffsetB);
				if(xEnd>xBegin) 
					proj.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
			} else {
				Color color = waveCanvas.styleProvider.getColor(WaveformColors.SIGNALX);
				int yOffset = yOffsetM;
				switch (((BitVector) left).getValue()[0]) {
				case '1':
					color = waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL1);
					yOffset = yOffsetT;
					break;
				case '0':
					color = waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL0);
					yOffset = yOffsetB;
					break;
				case 'Z':
					color = waveCanvas.styleProvider.getColor(WaveformColors.SIGNALZ);
					break;
				default:
				}
				proj.setForeground(color);
				if (xEnd > maxPosX) {
					proj.drawLine(xBegin, yOffset, maxPosX, yOffset);
				} else {
					proj.drawLine(xBegin, yOffset, xEnd, yOffset);
					int yNext = yOffsetM;
					switch (((BitVector) right).getValue()[0]) {
					case '1':
						yNext = yOffsetT;
						break;
					case '0':
						yNext = yOffsetB;
						break;
					default:
					}
					if (yOffset != yNext)
						proj.drawLine(xEnd, yOffset, xEnd, yNext);
				}
			}
		}
	}

	private class RealStencil implements SignalStencil {

		double minVal;
		double range;
		
		final double scaleFactor = 1.05;
		
		boolean continous=true;
		
		public RealStencil(IEventList entries, Object left, boolean continous) {
			this.continous=continous;
			Collection<EventEntry> values = entries.entrySet();
			minVal=((DoubleVal) left).value;
			range=2.0;
			if(!values.isEmpty()) {
				double maxVal=minVal;
				for (EventEntry val : values)
					for(IEvent e:val.events) {
						double v = ((DoubleVal)e).value;
						if(Double.isNaN(maxVal))
							maxVal=v;
						else if(!Double.isNaN(v))
							maxVal=Math.max(maxVal, v);
						if(Double.isNaN(minVal))
							minVal=v;
						else if(!Double.isNaN(v))
							minVal=Math.min(minVal, v);
					}
				if(Double.isNaN(maxVal)){
					maxVal=minVal=0.0;
				}
				range = (maxVal-minVal)*scaleFactor;
				double avg = (maxVal+minVal)/2.0;
				minVal=avg-(avg-minVal)*scaleFactor;
			}
		}

		public void draw(Projection proj, Rectangle area, IEvent left, IEvent right, int xBegin, int xEnd, boolean multiple) {
			double leftVal = ((DoubleVal) left).value;
			double rightVal= ((DoubleVal) right).value;
			if(Double.isNaN(leftVal)) {
				Color color = waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL_NAN);
				int width = xEnd - xBegin;
				if (width > 1) {
					int[] points = { 
							xBegin, yOffsetT, 
							xEnd,   yOffsetT, 
							xEnd,   yOffsetB, 
							xBegin, yOffsetB
					};
					proj.setForeground(color);
					proj.drawPolygon(points);
					proj.setBackground(color);
					proj.fillPolygon(points);
				} else {
					proj.setForeground(color);
					proj.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
				}
			} else {				
				proj.setForeground(waveCanvas.styleProvider.getColor(WaveformColors.SIGNAL_REAL));
				int yOffsetLeft = (int) ((leftVal-minVal) * (yOffsetB-yOffsetT) / range);
				int yOffsetRight = Double.isNaN(rightVal)?yOffsetLeft:(int) ((rightVal-minVal) * (yOffsetB-yOffsetT) / range);
				if(continous) {
					if (xEnd > maxPosX) {
						proj.drawLine(xBegin, yOffsetB-yOffsetLeft, maxPosX, yOffsetB-yOffsetRight);
					} else {
						proj.drawLine(xBegin, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetRight);
					}
				} else {
					if (xEnd > maxPosX) {
						proj.drawLine(xBegin, yOffsetB-yOffsetLeft, maxPosX, yOffsetB-yOffsetLeft);
					} else {
						proj.drawLine(xBegin, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetLeft);
						if(yOffsetRight!=yOffsetLeft) {
							proj.drawLine(xEnd, yOffsetB-yOffsetLeft, xEnd, yOffsetB-yOffsetRight);
						}
					}
				}
			}
		}
	}

}
