package com.minres.scviewer.database.ui.swt;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public interface ToolTipContentProvider {
	
	public boolean createContent(Composite parent, Point pt);

}