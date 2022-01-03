package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.ScrollBar;

public class ScrollBarAdapter implements IScrollBar {

	ScrollBar delegate;
	public ScrollBarAdapter(ScrollBar delegate) {
		this.delegate=delegate;
	}

	@Override
	public void setSelection(int i) {
		delegate.setSelection(i);
	}

	@Override
	public int getSelection() {
		return delegate.getSelection();
	}

	@Override
	public void setEnabled(boolean b) {
		delegate.setEnabled(b);
	}

	@Override
	public void setVisible(boolean b) {
		delegate.setVisible(b);
	}

	@Override
	public void addSelectionListener(SelectionListener selectionListener) {
		delegate.addSelectionListener(selectionListener);	}

	@Override
	public void setIncrement(int i) {
		delegate.setIncrement(i);
	}

	@Override
	public void setPageIncrement(int width) {
		delegate.setPageIncrement(width);
	}

	@Override
	public void setMinimum(int i) {
		delegate.setMinimum(i);
	}

	@Override
	public void setMaximum(int width) {
		delegate.setMaximum(width);
	}

	@Override
	public int getMaximum() {
		return delegate.getMaximum();
	}

	@Override
	public void setThumb(int clientWidthw) {
		delegate.setThumb(clientWidthw);
	}

	@Override
	public Point getSize() {
		return delegate.getSize();
	}

	@Override
	public int getStyle() {
		return delegate.getStyle();
	}

	@Override
	public boolean isVisible() {
		return delegate.isVisible();
	}

}
