package com.minres.scviewer.database.swt;

import org.eclipse.swt.widgets.Widget;

public interface ToolTipHelpTextProvider {
	/**
	 * Get help text
	 * @param widget the widget that is under help
	 * @return a help text string
	 */
	public String getHelpText(Widget widget);
}