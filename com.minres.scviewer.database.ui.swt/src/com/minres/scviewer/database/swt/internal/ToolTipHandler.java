package com.minres.scviewer.database.swt.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import com.minres.scviewer.database.swt.Constants;
import com.minres.scviewer.database.swt.ToolTipContentProvider;
import com.minres.scviewer.database.swt.ToolTipHelpTextProvider;

class ToolTipHandler {

	private final Display display;
	private Shell  parentShell;
	private Shell  shell;
	private Label  label;
	private Table  table;
	private TableColumn nameCol;
	private TableColumn valueCol;

	private Widget tipWidget; // widget this tooltip is hovering over
	private Point  tipPosition; // the position being hovered over

	private static final int hoverYOffset = 1;

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
			Shell tip = null;
			@Override
			public void handleEvent (Event event) {
				switch (event.type) {
				case SWT.Dispose:
				case SWT.KeyDown:
				case SWT.MouseMove: {
					if (tip == null) break;
					tip.dispose ();
					tip = null;
					label = null;
					break;
				}
				case SWT.MouseHover: {
					Object o = control.getData(Constants.CONTENT_PROVIDER_TAG);
					if(o != null && o instanceof ToolTipContentProvider) {
						ToolTipContentProvider provider = ((ToolTipContentProvider)o);
						Point pt = new Point (event.x, event.y);
						tipPosition = control.toDisplay(pt);
						if (tip != null  && !tip.isDisposed ()) tip.dispose ();
						tip = new Shell (parentShell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
						tip.setBackground (display.getSystemColor (SWT.COLOR_INFO_BACKGROUND));
						RowLayout layout=new RowLayout(SWT.VERTICAL);
						layout.fill=true;
						tip.setLayout(layout);
						boolean visible = provider.createContent(tip, pt);
						tip.pack();
						setHoverLocation(tip, tipPosition);	
						tip.setVisible (visible);
					}
				}
				}
			}
		};
		control.addListener (SWT.Dispose, listener);
		control.addListener (SWT.KeyDown, listener);
		control.addListener (SWT.MouseMove, listener);
		control.addListener (SWT.MouseHover, listener);

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
		//		control.addKeyListener(KeyListener.keyPressedAdapter( e-> {
		//				if (e.keyCode == SWT.F2 && shell.isVisible()) {
		//                    shell.setFocus();
		//                }
		//		}));
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
		shellBounds.y = Math.max(Math.min(position.y + hoverYOffset, displayBounds.height - shellBounds.height), 0);
		shell.setBounds(shellBounds);
	}
}