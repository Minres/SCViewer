package com.minres.scviewer.database.swt.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
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
	
	private Shell  parentShell;
	private Shell  tipShell;
	private Label  tipLabelText;
	private Table  tipTable;
	private Widget tipWidget; // widget this tooltip is hovering over
	private Point  tipPosition; // the position being hovered over

	private final TableColumn[] columns;
	
	private static final int hoverYOffset = 1;
	
	private static final String[] COLUMN_NAMES = { "Name", "Value"};
	
	
	private static final int MAX_CHARS = 48;
	// The names of the first 32 characters
	private Color[] colors = new Color[MAX_CHARS];
	
	/**
	 * Creates a new tooltip handler
	 *
	 * @param parent the parent Shell
	 */
	public ToolTipHandler(Shell parent) {
		final Display display = parent.getDisplay();
		this.parentShell = parent;

		tipShell = new Shell(parent, SWT.ON_TOP | SWT.TOOL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		tipShell.setLayout(gridLayout);

		tipShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		tipLabelText = new Label(tipShell, SWT.NONE);
		tipLabelText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		tipLabelText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		tipLabelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL |GridData.VERTICAL_ALIGN_CENTER));
		
		final Font font = new Font(Display.getCurrent(), "Terminal", 10, SWT.NORMAL);
		tipTable = new Table(tipShell, SWT.NONE);
		tipTable.setHeaderVisible(true);
		tipTable.setLinesVisible(true);
		tipTable.setFont(font);
		
		columns = createColumns(tipTable);

		tipTable.setRedraw(false);		
		for (int i = 0; i < MAX_CHARS; i++) {
			// Create a background color for this row
			colors[i] = new Color(tipTable.getDisplay(), 255 - i, 127 + i, i);

			// Create the row in the table by creating
			// a TableItem and setting text for each
			// column
			int c = 0;
			TableItem item = new TableItem(tipTable, SWT.NONE);
			item.setText(c++, String.valueOf((char) i));
			item.setText(c++, String.valueOf(i));
			item.setBackground(colors[i]);
		}
		// Now that we've set the text into the columns,
		// we call pack() on each one to size it to the
		// contents.
		for (int i = 0, n = columns.length; i < n; i++)
			columns[i].pack();
		// Set redraw back to true so that the table
		// will paint appropriately
		tipTable.setRedraw(true);

	}

	private TableColumn[] createColumns(Table table) {
		TableColumn[] columns = new TableColumn[COLUMN_NAMES.length];
		for (int i = 0, n = columns.length; i < n; i++) {
			columns[i] = new TableColumn(table, SWT.LEFT);
			columns[i].setText(COLUMN_NAMES[i]);
		}
		return columns;
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
			if (tipShell.isVisible())
				tipShell.setVisible(false);
		}));
		/*
		 * get out of the way if we move the mouse
		 */
		control.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (tipShell.isVisible())
					tipShell.setVisible(false);
			}

		});
		/*
		 * Trap hover events to pop-up tooltip
		 */
		control.addMouseTrackListener(new MouseTrackAdapter () {
			@Override
			public void mouseExit(MouseEvent e) {
				if (tipShell.isVisible()) tipShell.setVisible(false);
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
					tipShell.setVisible(false);
					tipWidget = null;
					return;
				}
				if (widget == tipWidget && tipShell.isVisible()) return;
				tipWidget = widget;
				tipPosition = control.toDisplay(pt);
				boolean showDialog = false;
				Object o = widget.getData(Constants.CONTENT_PROVIDER_TAG);
				if(o != null) {
					ToolTipTableContentProvider provider = ((ToolTipTableContentProvider)o).initialize(widget, pt);
					tipLabelText.setText(provider.getTableTitle());
					tipTable.setRedraw(false);	
					tipTable.removeAll();
					for (String[] strings : provider.getTableContent()) {
						if(strings.length>0) {
							showDialog=true;
							TableItem item = new TableItem(tipTable, SWT.NONE);
							item.setText(0, strings[0]);
							if(strings.length>1) 
								item.setText(1, strings[1]);
						}
					}
					for (int i = 0, n = columns.length; i < n; i++)
						columns[i].pack();
					tipTable.setRedraw(true);		
					tipTable.setVisible(true);
				} else {
					tipTable.setVisible(false);
				}
				String text = (String) widget.getData(Constants.TEXT_PROVIDER_TAG);
				if(text != null) {
					tipLabelText.setText(text != null ? text : "Hover test should go here");
					showDialog=true;
				}
				if(showDialog) {
					tipShell.pack();
					setHoverLocation(tipShell, tipPosition);
					tipShell.setVisible(true);
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

			if (tipShell.isVisible()) {
				tipShell.setVisible(false);
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
		shellBounds.y = Math.max(Math.min(position.y + hoverYOffset, displayBounds.height - shellBounds.height), 0);
		shell.setBounds(shellBounds);
	}
}