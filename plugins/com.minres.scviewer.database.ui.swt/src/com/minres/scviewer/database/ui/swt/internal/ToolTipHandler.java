package com.minres.scviewer.database.ui.swt.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.ToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.ToolTipHelpTextProvider;

class ToolTipHandler {

	private final Display display;
	private Shell  parentShell;
	private Shell  shell;

	private Widget tipWidget; // widget this tooltip is hovering over
	private Point  tipPosition; // the position being hovered over

	private static final int HOVER_YOFFSET = 1;

	/**
	 * Creates a new tooltip handler
	 *
	 * @param parent the parent Shell
	 */
	public ToolTipHandler(Shell parent) {
		display = parent.getDisplay();
		parentShell = parent;
	}

	/**
	 * Enables customized hover help for a specified control
	 *
	 * @control the control on which to enable hoverhelp
	 */
	public void activateHoverHelp(final Control control) {
		Listener listener = new Listener () {
			Shell shell = null;
			@Override
			public void handleEvent (Event event) {
				switch (event.type) {
				case SWT.KeyDown:
					if (shell != null && shell.isVisible() && event.keyCode == SWT.F2)
						shell.setFocus();
					break;
				case SWT.Dispose:
				case SWT.MouseMove:
				case SWT.MouseDown:
					if (shell != null){
						shell.dispose ();
						shell = null;
						tipWidget=null;
					}
					break;
				case SWT.MouseHover:
					createHoverWindow(control, event);
					break;
				default:
					/* do nothing */	
				}
			}
			private void createHoverWindow(final Control control, Event event) {
				Object o = control.getData(Constants.CONTENT_PROVIDER_TAG);
				if(o instanceof ToolTipContentProvider) {
					ToolTipContentProvider provider = ((ToolTipContentProvider)o);
					Point pt = new Point (event.x, event.y);
					tipPosition = control.toDisplay(pt);
					if (shell != null  && !shell.isDisposed ()) shell.dispose ();
					shell = new Shell (parentShell, SWT.NO_FOCUS | SWT.TOOL);
					shell.setBackground (display.getSystemColor (SWT.COLOR_INFO_BACKGROUND));
					GridLayout layout = new GridLayout(1, true);
					layout.verticalSpacing=0;
					layout.horizontalSpacing=0;
					layout.marginWidth = 0;
					layout.marginHeight = 0;
					shell.setLayout(layout);
					boolean visible = provider.createContent(shell, pt);
					shell.pack();
					shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					setHoverLocation(shell, tipPosition);	
					shell.setVisible (visible);
					if(visible)
						tipWidget=event.widget;
				}
			}
		};
		control.addListener (SWT.Dispose, listener);
		control.addListener (SWT.KeyDown, listener);
		control.addListener (SWT.MouseHover, listener);
		control.addListener (SWT.MouseDown, listener);

		/*
		 * Trap F1 Help to pop up a custom help box
		 */
		control.addHelpListener(event -> {
			if (tipWidget == null) return;
			ToolTipHelpTextProvider handler = (ToolTipHelpTextProvider)tipWidget.getData(Constants.HELP_PROVIDER_TAG);
			if (handler == null) return;
			String text = handler.getHelpText(tipWidget);
			if (text == null) return;

			if (shell.isVisible()) {
				shell.setVisible(false);
				Shell helpShell = new Shell(parentShell, SWT.SHELL_TRIM);
				helpShell.setLayout(new FillLayout());
				Label label = new Label(helpShell, SWT.NONE);
				label.setText(text);
				helpShell.pack();
				setHoverLocation(helpShell, tipPosition);
				helpShell.open();
			}
		});
	}

	/**
	 * Sets the location for a hovering shell
	 * @param shell the object that is to hover
	 * @param position the position of a widget to hover over
	 * @return the top-left location for a hovering box
	 */
	private void setHoverLocation(Shell shell, Point position) {
		Rectangle displayBounds = shell.getDisplay().getBounds();
		Rectangle shellBounds = shell.getBounds();
		shellBounds.x = Math.max(Math.min(position.x, displayBounds.width - shellBounds.width), 0);
		shellBounds.y = Math.max(Math.min(position.y + HOVER_YOFFSET, displayBounds.height - shellBounds.height), 0);
		shell.setBounds(shellBounds);
	}
}