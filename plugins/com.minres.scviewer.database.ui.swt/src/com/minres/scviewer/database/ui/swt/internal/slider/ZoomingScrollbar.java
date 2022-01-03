package com.minres.scviewer.database.ui.swt.internal.slider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

import com.minres.scviewer.database.ui.swt.internal.IScrollBar;

public class ZoomingScrollbar extends Composite implements IScrollBar {
	RangeSlider timeSlider;
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public ZoomingScrollbar(Composite parent, int style) {
		super(parent, SWT.NO_FOCUS);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);
		//setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));

		ImageButton b1 = new ImageButton(this, SWT.NONE);
		GridData gd_b1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_b1.widthHint=14;
		gd_b1.heightHint=18;
		b1.setLayoutData(gd_b1);
		b1.setImage(new Image[] {
				SWTResourceManager.getImage(this.getClass(), "arrow_left_hover.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_left.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_left_pressed.png")});
		b1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] value = timeSlider.getSelection();
				int diff = value[1]-value[0];
				int newLow = Math.max(0, value[0]-Math.max(1,  diff/10));
				timeSlider.setSelection(newLow, newLow+diff);
			}
		});
		timeSlider = new RangeSlider(this, /*SWT.ON|*/SWT.HIGH|SWT.SMOOTH|SWT.CONTROL);
		GridData gd_timeSlide = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		timeSlider.setLayoutData(gd_timeSlide);

		ImageButton b2 = new ImageButton(this, SWT.NONE);
		GridData gd_b2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_b2.widthHint=18;
		gd_b2.heightHint=18;
		b2.setLayoutData(gd_b2);
		b2.setImage(new Image[] {
				SWTResourceManager.getImage(this.getClass(), "arrow_right_hover.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_right.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_right_pressed.png")});
		b2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] value = timeSlider.getSelection();
				int diff = value[1]-value[0];
				int newHigh = Math.min(timeSlider.getMaximum(), value[1] + diff/10);
				timeSlider.setSelection(newHigh-diff, newHigh);
			}
		});

	}
	@Override
	public void setSelection(int i) {
		timeSlider.setLowerValue(i);
	}
	@Override
	public int getSelection() {
		return timeSlider.getLowerValue();
	}
	@Override
	public void addSelectionListener(SelectionListener selectionListener) {
		timeSlider.addSelectionListener(selectionListener);	
	}
	@Override
	public void setIncrement(int value) {
		timeSlider.setIncrement(value);	
	}
	@Override
	public void setPageIncrement(int value) {
		timeSlider.setPageIncrement(value);	
	}
	@Override
	public void setMinimum(int value) {
		timeSlider.setMinimum(value);	
	}
	@Override
	public void setMaximum(int value) {
		timeSlider.setMaximum(value);	
	}
	@Override
	public int getMaximum() {
		return timeSlider.getMaximum();
	}
	@Override
	public void setThumb(int w) {
		timeSlider.setUpperValue(timeSlider.getLowerValue()+w);
	}
}
