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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.FileBrowserDialog;
public class OpenHandler {

	@Execute
	public void execute(Shell shell, MApplication app, EModelService modelService, EPartService partService){
		FileBrowserDialog dlg = new FileBrowserDialog(shell);
		//dlg.create();
		dlg.setFilterExtensions (new String []{Messages.OpenHandler_0, "*"});
		if (dlg.open() != Window.OK) return;
		List<File> files = dlg.getSelectedFiles();
		MPart part = partService .createPart("com.minres.scviewer.e4.application.partdescriptor.waveformviewer"); //$NON-NLS-1$
		part.setLabel(files.get(0).getName());
		MPartStack partStack = (MPartStack)modelService.find("org.eclipse.editorss", app); //$NON-NLS-1$
		partStack.getChildren().add(part);
		partService.showPart(part, PartState.CREATE);
		final IEclipseContext ctx=part.getContext();
		List<String> inputs=new ArrayList<>();
		for(File f: files)
			inputs.add(f.getAbsolutePath());
		ctx.modify("input", inputs);
		ctx.modify("config", ""); //$NON-NLS-1$				
	}
	
}
