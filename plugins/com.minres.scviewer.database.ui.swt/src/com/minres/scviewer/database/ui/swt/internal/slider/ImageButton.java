package com.minres.scviewer.database.ui.swt.internal.slider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class ImageButton extends Composite
{
	private Color   textColor;
	private Image   image;
	private Image   grayImage;
	private ImageData imageData;
	private String  text = "";
	private int     width;
	private int     height;
	private boolean hover;

	public ImageButton(Composite parent, int style)
	{
		super(parent, style);

		textColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

		/* Add dispose listener for the image */
		addListener(SWT.Dispose, event ->  {
				if (image != null)
					image.dispose();
		});

		/* Add custom paint listener that paints the stars */
		addListener(SWT.Paint, event -> {
				paintControl(event);
		});

		/* Listen for click events */
		addListener(SWT.MouseDown, event -> {
				System.out.println("Click");
		});
		addListener(SWT.MouseDown, event -> {
		});

		addListener(SWT.MouseUp, event -> {
		});

		addListener(SWT.MouseMove, event -> {
			hover=false;
			redraw();
		});

		addListener(SWT.MouseWheel, event -> {
		});

		addListener(SWT.MouseHover, event -> {
			hover=true;
			redraw();
		});

		addListener(SWT.MouseDoubleClick, event -> {
		});
	}

	private void paintControl(Event event) { 
		GC gc = event.gc;

		if (image != null)
		{
//			gc.drawImage(image, 1, 1);
//			if(hover) {
//				Rectangle rect = image.getBounds ();
//				Transform tr = new Transform (event.display);
//				tr.setElements (1, 0, 0, -1, 1, 2*(1+rect.height));
//				gc.setTransform (tr);
//				gc.drawImage (image, 1, 1);
//				gc.setTransform (null);
//			}
			if(hover) {
				gc.drawImage(image, 1, 1);
			} else {
				gc.drawImage(grayImage, 1, 1);
			}
			Point textSize = gc.textExtent(text);
			gc.setForeground(textColor);
			gc.drawText(text, (width - textSize.x) / 2 + 1, (height - textSize.y) / 2 + 1, true);
		}
	}

	public void setImage(Image img)
	{
		image = new Image(Display.getDefault(), img, SWT.IMAGE_COPY);
		grayImage = new Image(Display.getDefault(),img,SWT.IMAGE_GRAY);
		width = img.getBounds().width;
		height = img.getBounds().height;
		imageData = img.getImageData();
		redraw();
	}

	public void setText(String text)
	{
		this.text = text;
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		int overallWidth = width;
		int overallHeight = height;

		/* Consider hints */
		if (wHint != SWT.DEFAULT && wHint < overallWidth)
			overallWidth = wHint;

		if (hHint != SWT.DEFAULT && hHint < overallHeight)
			overallHeight = hHint;

		/* Return computed dimensions plus border */
		return new Point(overallWidth + 2, overallHeight + 2);
	}

}
