/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.e4.application.parts.WaveformViewer;
public class ReloadHandler {

	@CanExecute
	public boolean canExecute(EPartService partService) {
		MPart part = partService.getActivePart();
		if(part==null) return false;
		return (part.getObject() instanceof WaveformViewer);
	}

	@Execute
	public void execute(Shell shell, MApplication app, EModelService modelService, 
			EPartService partService){
		
		MPart part = partService.getActivePart();
		Object obj = part == null ? null : part.getObject();
		if(obj instanceof WaveformViewer) {
			((WaveformViewer)obj).reloadDatabase();
		}
	    
	}
}