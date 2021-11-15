package com.minres.scviewer.e4.application.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class HelpDialog extends Dialog {
	/**
	 * Create the dialog.
	 *
	 * @param parentShell the parent shell
	 */
	@Inject
	public HelpDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.MODELESS | SWT.MAX | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	/**
	 * Create contents of the dialog.
	 *
	 * @param parent the parent
	 * @return the control
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		ToolBar toolbar = new ToolBar(container, SWT.NONE);
		ToolItem itemBack = new ToolItem(toolbar, SWT.PUSH);
		itemBack.setText("Back");
		ToolItem itemForward = new ToolItem(toolbar, SWT.PUSH);
		itemForward.setText("Forward");
		ToolItem itemStop = new ToolItem(toolbar, SWT.PUSH);
		itemStop.setText("Stop");
		ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		itemRefresh.setText("Refresh");
		ToolItem itemGo = new ToolItem(toolbar, SWT.PUSH);
		itemGo.setText("Go");

		GridData data = new GridData();
		data.horizontalSpan = 3;
		toolbar.setLayoutData(data);

		Label labelAddress = new Label(container, SWT.NONE);
		labelAddress.setText("Address");

		final Text location = new Text(container, SWT.BORDER);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		location.setLayoutData(data);

		final Browser browser;
		try {
			browser = new Browser(container, SWT.NONE);
			data = new GridData();
			//			data.widthHint = 800;
			//			data.heightHint =600;
			data.horizontalAlignment = GridData.FILL;
			data.verticalAlignment = GridData.FILL;
			data.horizontalSpan = 3;
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			browser.setLayoutData(data);

			final Label status = new Label(container, SWT.NONE);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			status.setLayoutData(data);

			final ProgressBar progressBar = new ProgressBar(container, SWT.NONE);
			data = new GridData();
			data.horizontalAlignment = GridData.END;
			progressBar.setLayoutData(data);

			/* event handling */
			Listener listener = event -> {
				ToolItem item = (ToolItem) event.widget;
				String string = item.getText();
				if (string.equals("Back"))
					browser.back();
				else if (string.equals("Forward"))
					browser.forward();
				else if (string.equals("Stop"))
					browser.stop();
				else if (string.equals("Refresh"))
					browser.refresh();
				else if (string.equals("Go"))
					browser.setUrl(location.getText());
			};
			browser.addProgressListener(new ProgressListener() {
				@Override
				public void changed(ProgressEvent event) {
					if (event.total == 0) return;
					int ratio = event.current * 100 / event.total;
					progressBar.setSelection(ratio);
				}
				@Override
				public void completed(ProgressEvent event) {
					progressBar.setSelection(0);
				}
			});
			browser.addStatusTextListener(event -> status.setText(event.text));
			browser.addLocationListener(LocationListener.changedAdapter(event -> {
				if (event.top) location.setText(event.location);
			}
					));
			itemBack.addListener(SWT.Selection, listener);
			itemForward.addListener(SWT.Selection, listener);
			itemStop.addListener(SWT.Selection, listener);
			itemRefresh.addListener(SWT.Selection, listener);
			itemGo.addListener(SWT.Selection, listener);
			location.addListener(SWT.DefaultSelection, e -> browser.setUrl(location.getText()));

			browser.setUrl("https://git.minres.com/VP-Tools/SCViewer/src/branch/master/README.md#key-shortcuts");
		} catch (SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
		}
		return container;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK button
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL,	true);
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	@PostConstruct
	@Override
	public int open() {
		return super.open();
	}

}