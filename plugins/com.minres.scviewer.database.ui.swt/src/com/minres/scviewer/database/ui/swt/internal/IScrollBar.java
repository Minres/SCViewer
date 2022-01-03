package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;

public interface IScrollBar {

	void setSelection(int i);

	int getSelection();

	void setEnabled(boolean b);

	void setVisible(boolean b);

	void addSelectionListener(SelectionListener selectionListener);

	void setIncrement(int i);

	void setPageIncrement(int width);

	void setMinimum(int i);

	void setMaximum(int width);

	int getMaximum();

	void setThumb(int clientWidthw);

	Point getSize();

	int getStyle();

	boolean isVisible();

}
