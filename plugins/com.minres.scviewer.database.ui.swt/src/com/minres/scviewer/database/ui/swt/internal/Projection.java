package com.minres.scviewer.database.ui.swt.internal;

import java.util.Arrays;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class Projection {

	private Point translation;
	private GC gc;

	public Projection(GC gc) {
		super();
		this.gc = gc;
		translation=new Point(0, 0);
	}

	void setTranslation(Point t) {
		translation = t;
	}

	void setGC(GC gc) {
		this.gc=gc;
	}

	Point getTranslation() {
		return translation;
	}

	Point project(Point p) {
		return new Point(p.x+translation.x, p.y+translation.y);
	}

	public Rectangle unProject(Rectangle r) {
		return new Rectangle(r.x-translation.x, r.y-translation.y, r.width, r.height);
	}

	public void setBackground(Color color) {
		gc.setBackground(color);
	}
	public void setFillRule(int rule) {
		gc.setFillRule(rule);
	}
	public void setLineStyle(int style) {
		gc.setLineStyle(style);
	}

	public void setLineWidth(int width) {
		gc.setLineWidth(width);
	}

	public void setForeground(Color color) {
		gc.setForeground(color);
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		gc.drawLine(x1+translation.x, y1+translation.y, x2+translation.x, y2+translation.y);
		
	}

	public GC getGC() {
		return gc;
	}

	public void fillRectangle(Rectangle rect) {
		gc.fillRectangle(rect.x+translation.x, rect.y+translation.y, rect.width, rect.height);
	}

	public void drawRectangle(Rectangle rect) {
		gc.drawRectangle(rect.x+translation.y, rect.y+translation.y, rect.width, rect.height);		
	}

	public void fillRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		gc.fillRoundRectangle(x+translation.x, y+translation.y, width, height, arcWidth, arcHeight);
	}

	public void drawRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		gc.drawRoundRectangle(x+translation.x, y+translation.y, width, height, arcWidth, arcHeight);	
	}

	public void setAntialias(int antialias) {
		gc.setAntialias(antialias);
	}

	public void drawText(String string, int x, int y) {
		gc.drawText(string, x+translation.x, y+translation.y);
	}

	private int[] project(int[] points) {
		int[] res = Arrays.copyOf(points, points.length);
		for(int i=0; i<points.length; i+=2) {
			res[i]=points[i]+translation.x;
			res[i+1]=points[i+1]+translation.y;
		}
		return res;
	}

	public void fillPolygon(int[] points) {
		gc.fillPolygon(project(points));
	}

	public void drawPolygon(int[] points) {
		gc.drawPolygon(project(points));
	}

	public Rectangle getClipping() {
		Rectangle c = gc.getClipping();
		return new Rectangle(c.x-translation.x, c.y-translation.y, c.width, c.height);
	}

	public void setClipping(int x, int y, int width, int height) {
		gc.setClipping(x+translation.x, y+translation.y, width, height);
	}

	public void setClipping(Rectangle r) {
		gc.setClipping(r.x+translation.x, r.y+translation.y, r.width, r.height);
	}

}
