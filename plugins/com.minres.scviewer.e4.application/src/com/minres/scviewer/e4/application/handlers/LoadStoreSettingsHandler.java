/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import java.io.File;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.WaveformViewer;
public class LoadStoreSettingsHandler {

	static final String PARAMETER_ID="com.minres.scviewer.e4.application.commandparameter.loadStore"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(EPartService partService) {
		MPart part = partService.getActivePart();
		if(part==null) return false;
		return (part.getObject() instanceof WaveformViewer);
	}

	@Execute
	public void execute(@Named(PARAMETER_ID) String param, Shell shell, MApplication app, EModelService modelService, 
			EPartService partService){
		
		boolean load = "load".equals(param); //$NON-NLS-1$
		FileDialog dialog = new FileDialog(shell, load?SWT.OPEN:SWT.SAVE);
		dialog.setFilterExtensions (new String []{Messages.LoadStoreSettingsHandler_2});
		if(!load) dialog.setFileName(Messages.LoadStoreSettingsHandler_3);
		
		String fileName = null;
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		
		// Save active tab settings
		if(!load) {
			// 3 possible cases when when saving active tab settings:
			// - user dismisses the dialog by pressing Cancel
			// - selected file name does not exist
			// - user agrees to overwrite existing file
			boolean done = false;
			while (!done) {
				// open the File Dialog
				fileName = dialog.open();
				if (fileName == null) {
					// user has cancelled -> quit and return
					done = true;
				} else {
					// user has selected a file -> see if it already exists
					File file = new File(fileName);
					if (file.exists()) {
						// file already exists -> asks for confirmation
						MessageBox mb = new MessageBox(dialog.getParent(), SWT.ICON_WARNING
								| SWT.YES | SWT.NO);
						mb.setText("Confirm overwrite");
						mb.setMessage(fileName + " already exists. Do you want to overwrite it?");
						// user clicks Yes -> all done, drop out 
						if(mb.open() == SWT.YES) {
							((WaveformViewer)obj).saveState(fileName);
							done = true;
						} else { // user clicks No -> redisplay the File Dialog
							done = false;
						}
					} else {
						// file does not exist -> save and drop out
						((WaveformViewer)obj).saveState(fileName);
						done = true;
					}
				}
			}
			return;
		} // end if(!load)
		
	    else { // load active tab settings
	    	String res = dialog.open();
	    	if(res != null && part!=null && (obj instanceof WaveformViewer)) { 
	        	((WaveformViewer)obj).loadState(res);
	    	}
	    }
	}
}