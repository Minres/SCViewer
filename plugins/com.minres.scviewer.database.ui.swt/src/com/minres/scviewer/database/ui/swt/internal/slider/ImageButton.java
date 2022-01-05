package com.minres.scviewer.database.ui.swt.internal.slider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

public class ImageButton extends Composite
{
	private Image   hoverImage;
	private Image   normalImage;
	private Image   pressedImage;
	private Image   disabledImage;
	private int     width;
	private int     height;
	private boolean hover;
	private boolean pressed;
	private boolean autoFire;
	private ActionTimer actionTimer;
	private ActionTimer.TimerAction timerAction;

	public ImageButton(Composite parent, int style)	{
		super(parent, style);		
		timerAction = new ActionTimer.TimerAction() {
			@Override
			public void run() {
				notifyListeners();
			}
			@Override
			public boolean isEnabled() {
				return pressed;
			}
		};
		actionTimer = new ActionTimer(timerAction, this.getDisplay() );
		addListener(SWT.Dispose, event ->  {
			if (hoverImage != null)	hoverImage.dispose();
			if (normalImage != null) normalImage.dispose();
			if (pressedImage != null) pressedImage.dispose();
			if (disabledImage != null) disabledImage.dispose();
		});
		addListener(SWT.Paint, event -> {
			paintControl(event);
		});
		addListener(SWT.MouseDown, event -> {
			if(!isEnabled()) return;
			pressed=true;
			notifyListeners();
		    if(autoFire) actionTimer.activate();
			redraw();
		});
		addListener(SWT.MouseUp, event -> {
			pressed=false;
			redraw();
		});
		addListener(SWT.MouseMove, event -> {
			if(!isEnabled()) return;
			Point sz = ((ImageButton)event.widget).getSize();
			final boolean within_x = event.x>0 && event.x<sz.x-1;
			final boolean within_y = event.y>0 && event.y<sz.y-1;
			hover= within_x && within_y;
			redraw();
		});
	}

	private void paintControl(Event event) { 
		GC gc = event.gc;
		if (hoverImage != null)	{
			if(pressed)
				gc.drawImage(pressedImage, 1, 1);
			else if(hover) {
				gc.drawImage(hoverImage, 1, 1);
			} else if(isEnabled()){
				gc.drawImage(normalImage, 1, 1);
			} else
				gc.drawImage(disabledImage, 1, 1);
		}
	}

	public void setImage(Image[] imgs) {
		assert(imgs.length==3);
		Display d = Display.getDefault();
		normalImage =   new Image(d, imgs[0], SWT.IMAGE_COPY);
		hoverImage =    new Image(d, imgs[1], SWT.IMAGE_COPY);
		pressedImage =  new Image(d, imgs[2], SWT.IMAGE_COPY);
		disabledImage = new Image(d, imgs[0], SWT.IMAGE_DISABLE);
		width = imgs[0].getBounds().width;
		height = imgs[0].getBounds().height;
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		int overallWidth = width;
		int overallHeight = height;
		if (wHint != SWT.DEFAULT && wHint < overallWidth)
			overallWidth = wHint;
		if (hHint != SWT.DEFAULT && hHint < overallHeight)
			overallHeight = hHint;
		return new Point(overallWidth + 2, overallHeight + 2);
	}
	/**
	 * Adds the listener to the collection of listeners who will be notified when
	 * the user changes the receiver's value, by sending it one of the messages
	 * defined in the <code>SelectionListener</code> interface.
	 * <p>
	 * <code>widgetSelected</code> is called when the user changes the receiver's
	 * value. <code>widgetDefaultSelected</code> is not called.
	 * </p>
	 *
	 * @param listener the listener which should be notified
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see SelectionListener
	 * @see #removeSelectionListener
	 */
	public void addSelectionListener(final SelectionListener listener) {
		checkWidget();
		SelectionListenerUtil.addSelectionListener(this, listener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified
	 * when the user changes the receiver's value.
	 *
	 * @param listener the listener which should no longer be notified
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see SelectionListener
	 * @see #addSelectionListener
	 */
	public void removeSelectionListener(final SelectionListener listener) {
		checkWidget();
		SelectionListenerUtil.removeSelectionListener(this, listener);
	}

	private void notifyListeners() {
		Event e = new Event();
		e.widget=this;
		e.type=SWT.Selection;
		SelectionListenerUtil.fireSelectionListeners(this,e);
	}

	public boolean isAutoFire() {
		return autoFire;
	}

	public void setAutoFire(boolean autoFire) {
		this.autoFire = autoFire;
	}
}
