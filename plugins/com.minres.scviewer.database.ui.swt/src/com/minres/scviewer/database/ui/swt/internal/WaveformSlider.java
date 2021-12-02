package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.wb.swt.SWTResourceManager;

public class WaveformSlider extends Composite {
	
	Slider slider;
	
	Color buttonColor;
	
	public WaveformSlider(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		buttonColor = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				
		Button scrlLeft = new Button(this, /*SWT.BORDER |*/ SWT.FLAT | SWT.CENTER);
		scrlLeft.setFont(SWTResourceManager.getFont("Sans", 5, SWT.NORMAL));
		GridData gd_scrlLeft = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_scrlLeft.heightHint = 16;
		gd_scrlLeft.widthHint = 16;
		scrlLeft.setLayoutData(gd_scrlLeft);
		scrlLeft.addPaintListener(paintEvent -> {
			GC gc = paintEvent.gc;
			gc.setBackground(buttonColor);
			gc.setForeground(buttonColor);
			int left = paintEvent.x+4;
			int top = paintEvent.y+5;
			int width=paintEvent.width-11;
			int height= paintEvent.height-10;
			int[] triangle = new int[] {
					left, top+height/2,
					left+width, top,
					left+width, top+height};
			gc.fillPolygon( triangle );
			gc.drawPolygon( triangle );
		});
		scrlLeft.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected (SelectionEvent e){
		    	slider.setSelection(slider.getSelection()-10);
		    }
		});
		scrlLeft.redraw();

		slider = new Slider(this, SWT.NONE);
		slider.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		GridData gd_canvas = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_canvas.heightHint = 16;
		slider.setLayoutData(gd_canvas);

		Button scrlRight = new Button(this, /*SWT.BORDER |*/ SWT.FLAT | SWT.CENTER);
		scrlRight.setAlignment(SWT.CENTER);
		GridData gd_scrlRight = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_scrlRight.heightHint = 16;
		gd_scrlRight.widthHint = 16;
		scrlRight.setLayoutData(gd_scrlRight);
		scrlRight.addPaintListener(paintEvent -> {
			GC gc = paintEvent.gc;
			gc.setBackground(buttonColor);
			gc.setForeground(buttonColor);
			int left = paintEvent.x+6;
			int top = paintEvent.y+5;
			int width=paintEvent.width-11;
			int height= paintEvent.height-10;
			int[] triangle = new int[] {
					left, top,
					left+width, top+height/2,
					left, top+height};
			gc.fillPolygon( triangle );
			gc.drawPolygon( triangle );
		});
		scrlRight.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected (SelectionEvent e){
		    	slider.setSelection(slider.getSelection()+10);
		    }
		});
		redraw();
	}
}
