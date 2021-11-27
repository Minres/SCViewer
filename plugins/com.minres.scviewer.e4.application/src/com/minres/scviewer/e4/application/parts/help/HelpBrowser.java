package com.minres.scviewer.e4.application.parts.help;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.minres.scviewer.e4.application.Messages;

public class HelpBrowser {

	@Inject IEclipseContext ctx;
	
	@Inject MUIElement element;
	
	private static void decorateItem(ToolItem item, String label, String imageName) {
		String fullpath = File.separator+"icons"+File.separator+imageName; //$NON-NLS-1$
		ImageDescriptor descr =  ResourceLocator.imageDescriptorFromBundle("com.minres.scviewer.e4.application", fullpath).orElse(null); //$NON-NLS-1$
		if(descr == null) {
			item.setText(label);
		} else {
			item.setImage(descr.createImage());
			item.setToolTipText(label);
		}
		item.setData(label);
	}

	@PostConstruct
	protected Control createComposite(Composite container) {
//		container.getShell().addListener(SWT.Close, e -> {
//			e.doit= false;
//			element.setVisible(false);
//		});
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		ToolBar toolbar = new ToolBar(container, SWT.NONE);
		ToolItem itemBack = new ToolItem(toolbar, SWT.PUSH);
		decorateItem(itemBack, Messages.HelpDialog_0, "arrow_undo.png"); //$NON-NLS-1$
		ToolItem itemForward = new ToolItem(toolbar, SWT.PUSH);
		decorateItem(itemForward, Messages.HelpDialog_1, "arrow_redo.png"); //$NON-NLS-1$
		ToolItem itemStop = new ToolItem(toolbar, SWT.PUSH);
		decorateItem(itemStop, Messages.HelpDialog_2, "cross.png"); //$NON-NLS-1$
		ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		decorateItem(itemRefresh, Messages.HelpDialog_3, "arrow_refresh.png"); //$NON-NLS-1$
		ToolItem itemGo = new ToolItem(toolbar, SWT.PUSH);
		decorateItem(itemGo, Messages.HelpDialog_4, "accept.png"); //$NON-NLS-1$

		GridData data = new GridData();
		data.horizontalSpan = 3;
		toolbar.setLayoutData(data);

		Label labelAddress = new Label(container, SWT.NONE);
		labelAddress.setText(Messages.HelpDialog_5);

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
				String string = (String) item.getData();
				if (string.equals(Messages.HelpDialog_0))
					browser.back();
				else if (string.equals(Messages.HelpDialog_1))
					browser.forward();
				else if (string.equals(Messages.HelpDialog_2))
					browser.stop();
				else if (string.equals(Messages.HelpDialog_3))
					browser.refresh();
				else if (string.equals(Messages.HelpDialog_4))
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
			browser.addLocationListener(LocationListener.changedAdapter(event -> { if (event.top) location.setText(event.location);	}));
			itemBack.addListener(SWT.Selection, listener);
			itemForward.addListener(SWT.Selection, listener);
			itemStop.addListener(SWT.Selection, listener);
			itemRefresh.addListener(SWT.Selection, listener);
			itemGo.addListener(SWT.Selection, listener);
			location.addListener(SWT.DefaultSelection, e -> browser.setUrl(location.getText()));
			browser.setUrl(Messages.HelpDialog_6);
		} catch (SWTError e) {
			MessageDialog.openWarning(container.getDisplay().getActiveShell(), Messages.HelpBrowser_7,Messages.HelpBrowser_8+e.getMessage());
		}
		return container;
	}
	
	void handleShellCloseEvent(){
		
	}
}