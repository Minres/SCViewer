package com.minres.scviewer.database.ui.swt.internal.slider;

import java.text.Format;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.wb.swt.SWTResourceManager;

public class RangeSlider extends Canvas {

	private static final int NONE = 0;
	private static final int UPPER = 1 << 0;
	private static final int LOWER = 1 << 1;
	private static final int BOTH = UPPER | LOWER;

	private final int minHeight;
	private final int markerWidth;
	private final int thumbWidth = 0;
	private final Image[] slider, sliderHover, sliderDrag;
	
	private int minimum;
	private int maximum;
	private int lowerValue;
	private int upperValue;
	
	private int increment;
	private int pageIncrement;
	private int selectedElement;
	private boolean upperHover, lowerHover;
	private int previousUpperValue, previousLowerValue;
	private int startDragUpperValue, startDragLowerValue;
	private Point startDragPoint;
	private final boolean isFullSelection=false;
	private final boolean isHighQuality;
	private final boolean isOn;
	private Format toolTipFormatter;
	private String clientToolTipText;
	private StringBuffer toolTip;
	private Point coordUpper;
	private Point coordLower;

	public RangeSlider(final Composite parent, final int style) {
		super(parent, SWT.DOUBLE_BUFFERED | ((style & SWT.BORDER) == SWT.BORDER ? SWT.BORDER : SWT.NONE));
		slider = new Image[] {
				SWTResourceManager.getImage(this.getClass(), "marker_l.png"),
				SWTResourceManager.getImage(this.getClass(), "marker_r.png")};
		sliderHover = new Image[] {
				SWTResourceManager.getImage(this.getClass(), "marker_l_hover.png"),
				SWTResourceManager.getImage(this.getClass(), "marker_r_hover.png")};
		sliderDrag = new Image[] {
				SWTResourceManager.getImage(this.getClass(), "marker_l_pressed.png"),
				SWTResourceManager.getImage(this.getClass(), "marker_r_pressed.png")};
		Rectangle imgSize = slider[0].getBounds();
		minHeight =imgSize.height+2;
		markerWidth = imgSize.width;
		minimum = lowerValue = 0;
		maximum = upperValue = 100;
		increment = 1;
		pageIncrement = 10;
		isHighQuality = (style & SWT.HIGH) == SWT.HIGH;
		isOn = (style & SWT.ON) == SWT.ON;
		selectedElement = NONE;

		addMouseListeners();
		addListener(SWT.Resize, event -> {
		});
		addListener(SWT.KeyDown, event -> {
			handleKeyDown(event);
		});
		addPaintListener(event -> {
			drawWidget(event);
		});
	}

	@Override
	public int getStyle() {
		return super.getStyle() | //
				(isOn ? SWT.ON : SWT.NONE) | //
				(isFullSelection ? SWT.CONTROL : SWT.NONE) | //
				(isHighQuality ? SWT.HIGH : SWT.NONE);
	}

	private void addMouseListeners() {
		addListener(SWT.MouseDown, e -> {
			if (e.button == 1) {
				selectKnobs(e);
				selectedElement = (lowerHover ? LOWER : NONE) | (upperHover ? UPPER : NONE);
				if (selectedElement!=NONE) {
					if((e.stateMask & SWT.CTRL)==0)
						selectedElement=BOTH;
					startDragLowerValue = previousLowerValue = lowerValue;
					startDragUpperValue = previousUpperValue = upperValue;
					startDragPoint = new Point(e.x, e.y);
				}
			}
		});

		addListener(SWT.MouseUp, e -> {
			if (selectedElement!=NONE) {
				startDragPoint = null;
				validateNewValues(e);
				super.setToolTipText(clientToolTipText);
				selectedElement=NONE;
				redraw();
			} else {
				if(e.x<coordLower.x) {
					translateValues(-pageIncrement);
					validateNewValues(e);
					redraw();
				} else if(e.x>coordUpper.x+markerWidth) {
					translateValues(pageIncrement);
					validateNewValues(e);
					redraw();
				}
			}
		});

		addListener(SWT.MouseDoubleClick, event -> {
			handleMouseDoubleClick(event);
		});

		addListener(SWT.MouseMove, event -> {
			handleMouseMove(event);
		});

		addListener(SWT.MouseWheel, event -> {
			handleMouseWheel(event);
		});

		addListener(SWT.MouseHover, event -> {
			handleMouseHover(event);
		});

		addMouseTrackListener(new MouseTrackAdapter() {
			public void mouseExit(MouseEvent event) {
				lowerHover = upperHover = false;
				redraw();
			}
		});

	}

	private void validateNewValues(final Event e) {
		if (upperValue != previousUpperValue || lowerValue != previousLowerValue) {
			if (!SelectionListenerUtil.fireSelectionListeners(this,e)) {
				upperValue = previousUpperValue;
				lowerValue = previousLowerValue;
			}
			previousUpperValue = upperValue;
			previousLowerValue = lowerValue;
			increment = Math.max(1,  (upperValue-lowerValue)/100);
			pageIncrement = Math.max(1, (upperValue-lowerValue)/2);
			redraw();
		}
	}

	private boolean busy = false;
	
	private void handleMouseMove(final Event e) {
		if (selectedElement==NONE) {
			final boolean wasUpper = upperHover;
			final boolean wasLower = lowerHover;
			selectKnobs(e);
			if (wasUpper != upperHover || wasLower != lowerHover) {
				redraw();
			}
		} else { // dragInProgress
			final int x = e.x;
			if (selectedElement == BOTH) {
				final int diff = (int) ((startDragPoint.x - x) / computePixelSizeForSlider()) + minimum;
				int newUpper = startDragUpperValue - diff;
				int newLower = startDragLowerValue - diff;
				if (newUpper > maximum) {
					newUpper = maximum;
					newLower = maximum - (startDragUpperValue - startDragLowerValue);
				} else if (newLower < minimum) {
					newLower = minimum;
					newUpper = minimum + startDragUpperValue - startDragLowerValue;
				}
				upperValue = newUpper;
				lowerValue = newLower;
				handleToolTip(lowerValue, upperValue);
			} else if (selectedElement == UPPER) {
				upperValue = (int) Math.round((double)(x - markerWidth) / computePixelSizeForSlider()) + minimum;
				checkUpperValue();
				handleToolTip(upperValue);
			} else if (selectedElement == LOWER){
				lowerValue = (int) Math.round((double)(x - markerWidth) / computePixelSizeForSlider()) + minimum;
				checkLowerValue();
				handleToolTip(lowerValue);
			}
			if (isOn && !busy) {
				validateNewValues(e);
				busy=true;
				getDisplay().timerExec(50, ()->{busy=false;});
			} else {
				redraw();
			}
		}
	}

	private boolean isBetweenKnobs(int x, int y) {
		return x < coordUpper.x && x > coordLower.x && y >= minHeight/3 && y <= minHeight/3 + getClientArea().height - 2*minHeight/3;
	}

	private void selectKnobs(final Event e) {
		if (coordLower != null) {
			final Rectangle imgBounds = slider[0].getBounds();
			final int x = e.x, y = e.y;
			lowerHover = x >= coordLower.x && x <= coordLower.x + imgBounds.width && y >= coordLower.y && y <= coordLower.y + imgBounds.height;
			upperHover = ((e.stateMask & (SWT.CTRL | SWT.SHIFT)) != 0 || !lowerHover) && //
					x >= coordUpper.x && x <= coordUpper.x + imgBounds.width && //
					y >= coordUpper.y && y <= coordUpper.y + imgBounds.height;
					lowerHover &= (e.stateMask & SWT.CTRL) != 0 || !upperHover;
			if (!lowerHover && !upperHover && isBetweenKnobs(x, y)) {
				lowerHover = upperHover = true;
			}		
		}
	}

	private int getCursorValue(int x, int y) {
		int value = -1;
		final Rectangle clientArea = getClientArea();
		if (x < clientArea.width - 2*markerWidth && x >= markerWidth && y >= minHeight/3 && y <=  clientArea.height - minHeight/3) {
			value = (int) Math.round((x - 9d) / computePixelSizeForSlider()) + minimum;
		}
		return value;
	}

	private void handleMouseDoubleClick(final Event e) {
		final int value = getCursorValue(e.x, e.y);
		if (value >= 0) {
			if (value > upperValue) {
				translateValues(value-upperValue);
			} else if (value < lowerValue) {
				translateValues(value-lowerValue);
			}
			validateNewValues(e);
		}
	}

	private void handleToolTip(int... values) {
		if (toolTipFormatter != null) {
			try {
				if (values.length == 1) {
					toolTip.setLength(0);
					toolTipFormatter.format(values[0], toolTip, null);
					super.setToolTipText(toolTip.toString());
				} else if (values.length == 2) {
					toolTip.setLength(0);
					toolTipFormatter.format(values[0], toolTip, null);
					toolTip.append(" \u2194 "); // LEFT RIGHT ARROW
					toolTipFormatter.format(values[1], toolTip, null);
					super.setToolTipText(toolTip.toString());
				}
			} catch (final IllegalArgumentException ex) {
				super.setToolTipText(clientToolTipText);
			}
		}
	}

	private void handleMouseHover(final Event e) {
		if (selectedElement!=NONE && toolTipFormatter != null) {
			final int value = getCursorValue(e.x, e.y);
			if (value >= 0) {
				try {
					toolTip.setLength(0);
					toolTipFormatter.format(value, toolTip, null);
					super.setToolTipText(toolTip.toString());
				} catch (final IllegalArgumentException ex) {
					super.setToolTipText(clientToolTipText);
				}
			} else {
				super.setToolTipText(clientToolTipText);
			}
		}
	}

	public void setToolTipFormatter(Format formatter) {
		toolTip = formatter != null ? new StringBuffer() : null;
		toolTipFormatter = formatter;
	}

	@Override
	public void setToolTipText(String string) {
		super.setToolTipText(clientToolTipText = string);
	}

	private void handleMouseWheel(final Event e) {
		previousLowerValue = lowerValue;
		previousUpperValue = upperValue;
		final int amount = Math.max(1, ((e.stateMask & SWT.SHIFT) != 0 ? (upperValue-lowerValue)/6 : (upperValue-lowerValue)/15));
		if ((e.stateMask&SWT.CTRL)==0) {
			int newLower = lowerValue + e.count * amount;
			int newUpper = upperValue + e.count * amount;
			if (newUpper > maximum) {
				newUpper = maximum;
				newLower = maximum - (upperValue - lowerValue);
			} else if (newLower < minimum) {
				newLower = minimum;
				newUpper = minimum + upperValue - lowerValue;
			}
			upperValue = newUpper;
			lowerValue = newLower;
		} else {
			int newLower = lowerValue + e.count * amount/2;
			int newUpper = upperValue - e.count * amount/2;
			int dist = newUpper - newLower;
			if (newUpper > maximum) {
				newUpper = maximum;
				newLower = maximum - dist;
			} else if (newLower < minimum) {
				newLower = minimum;
				newUpper = minimum + dist;
			}
			if(newUpper<=newLower) {
				newLower=lowerValue + (upperValue - lowerValue)/2;
				newUpper=newLower+1;
			}
			upperValue = newUpper;
			lowerValue = newLower;
		}
		validateNewValues(e);
		e.doit = false; // we are consuming this event
	}

	private void checkLowerValue() {
		if (lowerValue < minimum) {
			lowerValue = minimum;
		} else if (lowerValue > (upperValue-thumbWidth)) {
			lowerValue = (upperValue-thumbWidth);
		}
	}

	private void checkUpperValue() {
		if (upperValue > maximum) {
			upperValue = maximum;
		} else if (upperValue < (lowerValue+thumbWidth)) {
			upperValue = lowerValue+thumbWidth;
		}
	}

	private float computePixelSizeForSlider() {
		return (getClientArea().width - 2.0f*markerWidth) / (maximum - minimum);
	}

	private void drawWidget(final PaintEvent e) {
		final Rectangle rect = getClientArea();
		if (rect.width == 0 || rect.height == 0) {
			return;
		}
		e.gc.setAdvanced(true);
		e.gc.setAntialias(SWT.ON);
		drawBackground(e.gc);
		if (lowerHover || (selectedElement & LOWER) != 0) {
			coordUpper = drawMarker(e.gc, upperValue, true);
			coordLower = drawMarker(e.gc, lowerValue, false);
		} else {
			coordLower = drawMarker(e.gc, lowerValue, false);
			coordUpper = drawMarker(e.gc, upperValue, true);
		}
	}

	private void drawBackground(final GC gc) {
		final Rectangle clientArea = getClientArea();
		gc.setBackground(getBackground());
		gc.fillRectangle(clientArea);
		if (isEnabled()) {
			gc.setForeground(getForeground());
		} else {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
		}
		gc.drawRoundRectangle(markerWidth, minHeight/3, clientArea.width - 2*markerWidth, clientArea.height -  2*minHeight/3, 3, 3);

		final float pixelSize = computePixelSizeForSlider();
		final int startX = (int) (pixelSize * lowerValue);
		final int endX = (int) (pixelSize * upperValue);
		if (isEnabled()) {
			gc.setBackground(getForeground());
		} else {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
		}
		gc.fillRectangle(markerWidth+startX, minHeight/3, endX - startX, clientArea.height - 2*minHeight/3);

	}

	private Point drawMarker(final GC gc, final int value, final boolean upper) {
		final float pixelSize = computePixelSizeForSlider();
		int x = (int) (pixelSize * value);
		final int idx = upper?1:0;
		Image image;
		if (upper) {
			if (upperHover) {
				image = (selectedElement & UPPER) != 0 ? sliderDrag[idx] : sliderHover[idx];
			} else {
				image = slider[idx];
			}
		} else {
			if (lowerHover) {
				image = (selectedElement & LOWER) != 0 ? sliderDrag[idx] : sliderHover[idx];
			} else {
				image = slider[idx];
			}
		}
		if(upper)
			x+=slider[idx].getBounds().width;
		if (isEnabled()) {
			gc.drawImage(image, x, getClientArea().height / 2 - slider[idx].getBounds().height / 2);
		} else {
			final Image temp = new Image(getDisplay(), image, SWT.IMAGE_DISABLE);
			gc.drawImage(temp, x, getClientArea().height / 2 - slider[idx].getBounds().height / 2);
			temp.dispose();
		}
		return new Point(x, getClientArea().height / 2 - slider[idx].getBounds().height / 2);
	}

	private void moveCursorPosition(int xDelta, int yDelta) {
		final Point cursorPosition = getDisplay().getCursorLocation();
		cursorPosition.x += xDelta;
		cursorPosition.y += yDelta;
		getDisplay().setCursorLocation(cursorPosition);
	}

	private void handleKeyDown(final Event event) {
		int accelerator = (event.stateMask & SWT.SHIFT) != 0 ? 10 : (event.stateMask & SWT.CTRL) != 0 ? 2 : 1;
		if (selectedElement != NONE) {
			switch (event.keyCode) {
			case SWT.ESC:
				startDragPoint = null;
				upperValue = startDragUpperValue;
				lowerValue = startDragLowerValue;
				validateNewValues(event);
				selectedElement = NONE;
				if (!isOn) {
					redraw();
				}
				event.doit = false;
				break;
			case SWT.ARROW_UP:
				accelerator = -accelerator;
			case SWT.ARROW_LEFT:
				moveCursorPosition(-accelerator, 0);
				event.doit = false;
				break;
			case SWT.ARROW_DOWN:
				accelerator = -accelerator;
			case SWT.ARROW_RIGHT:
				moveCursorPosition(accelerator, 0);
				event.doit = false;
				break;
			}
			return;
		}
		previousLowerValue = lowerValue;
		previousUpperValue = upperValue;

		switch (event.keyCode) {
		case SWT.HOME:
			if ((event.stateMask & (SWT.SHIFT| SWT.CTRL)) == 0) {
				upperValue = minimum + upperValue - lowerValue;
				lowerValue = minimum;
			}
			break;
		case SWT.END:
			if ((event.stateMask & (SWT.SHIFT| SWT.CTRL)) == 0) {
				lowerValue = maximum - (upperValue - lowerValue);
				upperValue = maximum;
			}
			break;
		case SWT.PAGE_UP:
			translateValues(-accelerator * pageIncrement);
			break;
		case SWT.PAGE_DOWN:
			translateValues( accelerator * pageIncrement);
			break;
		case SWT.ARROW_DOWN:
		case SWT.ARROW_RIGHT:
			translateValues( accelerator * increment);
			break;
		case SWT.ARROW_UP:
		case SWT.ARROW_LEFT:
			translateValues(-accelerator * increment);
			break;
		}
		if (previousLowerValue != lowerValue || previousUpperValue != upperValue) {
			checkLowerValue();
			checkUpperValue();
			validateNewValues(event);
		}
	}

	private void translateValues(int amount) {
		int newLower = lowerValue + amount;
		int newUpper = upperValue + amount;
		if (newUpper > maximum) {
			newUpper = maximum;
			newLower = maximum - (upperValue - lowerValue);
		} else if (newLower < minimum) {
			newLower = minimum;
			newUpper = minimum + upperValue - lowerValue;
		}
		upperValue = newUpper;
		lowerValue = newLower;
	}

	public void addSelectionListener(final SelectionListener listener) {
		checkWidget();
		SelectionListenerUtil.addSelectionListener(this, listener);
	}

	public void removeSelectionListener(final SelectionListener listener) {
		checkWidget();
		SelectionListenerUtil.removeSelectionListener(this, listener);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		checkWidget();
		final int width = Math.max(2*markerWidth+100, wHint);
		final int height = Math.max(minHeight, hHint);
		return new Point(width, height);
	}

	public int[] getSelection() {
		checkWidget();
		return new int[] {lowerValue, upperValue};
	}

	public int getIncrement() {
		checkWidget();
		return increment;
	}

	public int getMaximum() {
		checkWidget();
		return maximum;
	}

	public int getMinimum() {
		checkWidget();
		return minimum;
	}

	public int getPageIncrement() {
		checkWidget();
		return pageIncrement;
	}

	public void setMaximum(final int value) {
		setLimits(minimum, value);
	}

	public void setMinimum(final int value) {
		setLimits(value, maximum);
	}

	public void setLimits(final int min, final int max) {
		checkWidget();
		if (min >= 0 && min < max && (min != minimum || max != maximum)) {
			minimum = min;
			maximum = max;
			if (lowerValue < minimum) {
				lowerValue = minimum;
			} else if (lowerValue > maximum) {
				lowerValue = maximum;
			}
			if (upperValue < minimum) {
				upperValue = minimum;
			} else if (upperValue > maximum) {
				upperValue = maximum;
			}
			redraw();
		}
	}

	public int getUpperValue() {
		checkWidget();
		return upperValue;
	}

	public void setUpperValue(final int value) {
		setValues(lowerValue, value);
	}

	public int getLowerValue() {
		checkWidget();
		return lowerValue;
	}

	public void setLowerValue(final int value) {
		setValues(value, upperValue);
	}

	public void setValues(final int[] values) {
		if (values.length == 2) {
			setValues(values[0], values[1]);
		}
	}

	public void setValues(final int lowerValue, final int upperValue) {
		setValues(lowerValue, upperValue, false);
	}

	public void setValues(final int lowerValue, final int upperValue, boolean update) {
		checkWidget();
		if (lowerValue <= upperValue && lowerValue >= minimum && upperValue <= maximum && (this.lowerValue != lowerValue || this.upperValue != upperValue)) {
			this.lowerValue = lowerValue;
			this.upperValue = upperValue;
			if(update) {
				Event e = new Event();
				e.type=SWT.Selection;
				e.doit=true;
				validateNewValues(e);			
			} else {
				increment = Math.max(1,  (upperValue-lowerValue)/100);
				pageIncrement = Math.max(1, (upperValue-lowerValue)/2);
			}
			redraw();
		}
	}
}