package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.layout.GridData;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FillLayout;

public class ZoomingScrollbar extends Composite {

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public ZoomingScrollbar(Composite parent, int style) {
		super(parent, SWT.BORDER | SWT.NO_FOCUS);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);
		
		Button scrlLeft = new Button(this, SWT.BORDER | SWT.FLAT | SWT.CENTER);
		scrlLeft.setFont(SWTResourceManager.getFont("Sans", 5, SWT.NORMAL));
		GridData gd_scrlLeft = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_scrlLeft.heightHint = 16;
		gd_scrlLeft.widthHint = 16;
		scrlLeft.setLayoutData(gd_scrlLeft);
		
		Slider slider = new Slider(this, SWT.NONE);
		slider.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		GridData gd_canvas = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_canvas.heightHint = 16;
		slider.setLayoutData(gd_canvas);
		
		Button scrlRight = new Button(this, SWT.BORDER | SWT.FLAT | SWT.CENTER);
		scrlRight.setAlignment(SWT.CENTER);
		GridData gd_scrlRight = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_scrlRight.heightHint = 16;
		gd_scrlRight.widthHint = 16;
		scrlRight.setLayoutData(gd_scrlRight);
		
		SashForm sashForm = new SashForm(this, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		Composite composite = new Composite(sashForm, SWT.NONE);
		composite.setBackground(SWTResourceManager.getColor(SWT.COLOR_MAGENTA));
		composite.setLayout(null);
		
		Composite wavformPane = new Composite(sashForm, SWT.BORDER | SWT.NO_FOCUS);
		wavformPane.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		GridLayout gl_wavformPane = new GridLayout(1, false);
		gl_wavformPane.verticalSpacing = 0;
		gl_wavformPane.marginWidth = 0;
		gl_wavformPane.marginHeight = 0;
		wavformPane.setLayout(gl_wavformPane);
		
		Composite waveformCanvas = new Composite(wavformPane, SWT.NONE);
		waveformCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite timeSlider = new Composite(wavformPane, SWT.BORDER | SWT.NO_FOCUS);
		GridLayout gl_timeSlider = new GridLayout(3, false);
		gl_timeSlider.marginWidth = 0;
		gl_timeSlider.verticalSpacing = 0;
		gl_timeSlider.marginHeight = 0;
		gl_timeSlider.horizontalSpacing = 0;
		timeSlider.setLayout(gl_timeSlider);
		GridData gd_timeSlider = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
		gd_timeSlider.heightHint = 16;
		timeSlider.setLayoutData(gd_timeSlider);
		
		Button buttonLeft = new Button(timeSlider, SWT.BORDER | SWT.FLAT | SWT.CENTER);
		buttonLeft.setFont(SWTResourceManager.getFont("Sans", 5, SWT.NORMAL));
		GridData gd_buttonLeft = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_buttonLeft.widthHint = 10;
		gd_buttonLeft.heightHint = 16;
		buttonLeft.setLayoutData(gd_buttonLeft);
		
		Slider slider2 = new Slider(timeSlider, SWT.NONE);
		slider2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		
		Button buttonRight = new Button(timeSlider, SWT.FLAT | SWT.CENTER);
		buttonRight.setFont(SWTResourceManager.getFont("Sans", 5, SWT.NORMAL));
		GridData gd_buttonRight = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_buttonRight.widthHint = 10;
		gd_buttonRight.heightHint = 16;
		buttonRight.setLayoutData(gd_buttonRight);
		sashForm.setWeights(new int[] {1, 1});

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
