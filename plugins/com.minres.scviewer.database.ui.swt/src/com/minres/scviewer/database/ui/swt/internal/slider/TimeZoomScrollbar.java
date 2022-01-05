package com.minres.scviewer.database.ui.swt.internal.slider;

import java.text.Format;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

public class TimeZoomScrollbar extends Composite {
	
	static public interface IProvider {
		TimeZoomScrollbar getScrollBar();
	}

	final RangeSlider timeSlider;
	final ImageButton leftButton;
	final ImageButton rightButton;
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TimeZoomScrollbar(Composite parent, int style) {
		super(parent, SWT.NO_FOCUS);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		leftButton = new ImageButton(this, SWT.NONE);
		GridData gd_leftButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_leftButton.widthHint=14;
		gd_leftButton.heightHint=18;
		leftButton.setLayoutData(gd_leftButton);
		leftButton.setImage(new Image[] {
				SWTResourceManager.getImage(this.getClass(), "arrow_left.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_left_hover.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_left_pressed.png")});
		leftButton.setAutoFire(true);
		
		timeSlider = new RangeSlider(this, SWT.ON|SWT.HIGH|SWT.SMOOTH|SWT.CONTROL);
		timeSlider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		rightButton = new ImageButton(this, SWT.NONE);
		GridData gd_rightButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_rightButton.widthHint=18;
		gd_rightButton.heightHint=18;
		rightButton.setLayoutData(gd_rightButton);
		rightButton.setImage(new Image[] {
				SWTResourceManager.getImage(this.getClass(), "arrow_right.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_right_hover.png"),
				SWTResourceManager.getImage(this.getClass(), "arrow_right_pressed.png")});
		rightButton.setAutoFire(true);
		
		leftButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] value = timeSlider.getSelection();
				int diff = value[1]-value[0];
				int newLow = Math.max(0, value[0]-Math.max(1,  diff/10));
				timeSlider.setSelection(newLow, newLow+diff, true);
			}
		});
		rightButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] value = timeSlider.getSelection();
				int diff = value[1]-value[0];
				int newHigh = Math.min(timeSlider.getMaximum(), value[1] + diff/10);
				timeSlider.setSelection(newHigh-diff, newHigh, true);
			}
		});

	}
	@Override
	public void setEnabled (boolean enabled) {
		timeSlider.setEnabled(enabled);
		leftButton.setEnabled(enabled);
		rightButton.setEnabled(enabled);
		super.setEnabled(enabled);
		redraw();
	}
	public void setButtonsEnabled (boolean enabled) {
		leftButton.setEnabled(enabled);
		rightButton.setEnabled(enabled);
		redraw();
	}
	public void setToolTipFormatter(Format formatter){
		timeSlider.setToolTipFormatter(formatter);
	}
	public void setToolTipText(String string) {
		timeSlider.setToolTipText(string);
	}
	public void setSelection(int sel) {
		timeSlider.setLowerValue(sel);
	}
	public void setSelection(int[] sel) {
		assert(sel.length==2);
		timeSlider.setSelection(sel[0], sel[1]);
	}
	public int[] getSelection() {
		return timeSlider.getSelection();
	}
	public void addSelectionListener(SelectionListener selectionListener) {
		timeSlider.addSelectionListener(selectionListener);	
	}
	public void setIncrement(int value) {
		timeSlider.setIncrement(value);	
	}
	public void setPageIncrement(int value) {
		timeSlider.setPageIncrement(value);	
	}
	public void setMinimum(int value) {
		timeSlider.setMinimum(value);	
	}
	public void setMaximum(int value) {
		timeSlider.setMaximum(value);	
	}
	public int getMaximum() {
		return timeSlider.getMaximum();
	}
	public int getMinimum() {
		return timeSlider.getMinimum();
	}
}
