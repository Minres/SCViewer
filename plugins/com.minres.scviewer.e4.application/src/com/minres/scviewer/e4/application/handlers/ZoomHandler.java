/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
 
package com.minres.scviewer.e4.application.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import com.minres.scviewer.database.ui.ZoomKind;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class ZoomHandler {

	final static String PARAMTER_ID="com.minres.scviewer.e4.application.command.zoomcommand.parameter.level"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(EPartService partService) {
		return true;
	}
		
	@Execute
	public void execute(@Named(PARAMTER_ID) String level, EPartService partService) {
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer waveformViewerPart = (WaveformViewer) obj;
			if("in".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.zoom(ZoomKind.IN);
			else if("out".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.zoom(ZoomKind.OUT);
			else if("fit".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.zoom(ZoomKind.FIT);
			else if("full".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.zoom(ZoomKind.FULL);
		}

	}
	
	
}