package com.minres.scviewer.database.swt;

import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Widget;

public interface ToolTipTableContentProvider {
	
	public ToolTipTableContentProvider initialize(Widget widget, Point pt);
	
	public String getTableTitle();
	/**
	 * Get tool tip table content
	 * @param widget the widget that is under help
	 * @oaram pt the point where the mouse cursor is located
	 * @return a list of string arrays of size 2 (content & value)
	 */
	public List<String[]> getTableContent();

}