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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import com.minres.scviewer.database.swt.Constants;
import com.minres.scviewer.database.swt.ToolTipHelpTextProvider;
import com.minres.scviewer.database.swt.ToolTipTableContentProvider;

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
		this.parentShell = parent;

		shell = new Shell(parent, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		shell.setLayout(gridLayout);

		shell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		label = new Label(shell, SWT.NONE);
		label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL |GridData.VERTICAL_ALIGN_CENTER));
		
		final Font font = new Font(Display.getCurrent(), "Terminal", 10, SWT.NORMAL);
		table = new Table(shell, SWT.NONE);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		table.setFont(font);
		table.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		table.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		nameCol = new TableColumn(table, SWT.LEFT);
		nameCol.setText("Name");
		valueCol = new TableColumn(table, SWT.LEFT);
		nameCol.setText("Value");
		
		shell.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle area = shell.getClientArea();
				valueCol.setWidth(area.width - nameCol.getWidth());
			}
		});
	}

	/**
	 * Enables customized hover help for a specified control
	 *
	 * @control the control on which to enable hoverhelp
	 */
	public void activateHoverHelp(final Control control) {
		/*
		 * Get out of the way if we attempt to activate the control underneath the tooltip
		 */
		control.addMouseListener(MouseListener.mouseDownAdapter(e -> {
			if (shell.isVisible())
				shell.setVisible(false);
		}));
		/*
		 * Trap hover events to pop-up tooltip
		 */
		control.addMouseTrackListener(new MouseTrackAdapter () {
			@Override
			public void mouseExit(MouseEvent e) {
				if (shell.isVisible()) shell.setVisible(false);
				tipWidget = null;
			}
			@Override
			public void mouseHover (MouseEvent event) {
				Point pt = new Point (event.x, event.y);
				Widget widget = event.widget;
				if (widget instanceof ToolBar) {
					ToolBar w = (ToolBar) widget;
					widget = w.getItem (pt);
				}
				if (widget instanceof Table) {
					Table w = (Table) widget;
					widget = w.getItem (pt);
				}
				if (widget instanceof Tree) {
					Tree w = (Tree) widget;
					widget = w.getItem (pt);
				}
				if (widget == null) {
					shell.setVisible(false);
					tipWidget = null;
					return;
				}
				Point newPos = control.toDisplay(pt);
				if(shell.isFocusControl()) return;
				if (widget == tipWidget && tipPosition.equals(newPos) && shell.isVisible()) return;
				tipWidget = widget;
				tipPosition = newPos;
				boolean showDialog = false;
				Object o = widget.getData(Constants.CONTENT_PROVIDER_TAG);
				if(o != null) {
					ToolTipTableContentProvider provider = ((ToolTipTableContentProvider)o).initialize(widget, pt);
					label.setText(provider.getTableTitle());
					table.setRedraw(false);	
					table.removeAll();
					for (String[] strings : provider.getTableContent()) {
						if(strings.length>0) {
							showDialog=true;
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(0, strings[0]);
							if(strings.length>1) 
								item.setText(1, strings[1]);
						}
					}
					nameCol.pack();
					valueCol.pack();
					table.setRedraw(true);		
					table.setVisible(true);
				} else {
					table.setVisible(false);
				}
				String text = (String) widget.getData(Constants.TEXT_PROVIDER_TAG);
				if(text != null) {
					label.setText(text != null ? text : "Hover test should go here");
					showDialog=true;
				}
				if(showDialog) {
					shell.pack();
					setHoverLocation(shell, tipPosition);
					shell.setVisible(true);
				}
			}
		});

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