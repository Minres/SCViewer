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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.e4.application.Messages;
public class OpenHandler {

	@Execute
	public void execute(Shell shell, MApplication app, EModelService modelService, EPartService partService){
		FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions (new String []{Messages.OpenHandler_0});
		dialog.open();
		String path = dialog.getFilterPath();
		for(String fileName: dialog.getFileNames()){
			File file = new File(path+File.separator+fileName);
			if(file.exists()){
				MPart part = partService .createPart("com.minres.scviewer.e4.application.partdescriptor.waveformviewer"); //$NON-NLS-1$
				part.setLabel(file.getName());
				MPartStack partStack = (MPartStack)modelService.find("org.eclipse.editorss", app); //$NON-NLS-1$
				partStack.getChildren().add(part);
				partService.showPart(part, PartState.ACTIVATE);
				IEclipseContext ctx=part.getContext();
				ctx.modify("input", file); //$NON-NLS-1$
				ctx.modify("config", ""); //$NON-NLS-1$				
			}
		}
	}
	
}
