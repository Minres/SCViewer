/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
 
package com.minres.scviewer.e4.application.handlers;

import javax.inject.Named;

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
				waveformViewerPart.setZoom(ZoomKind.IN);
			else if("out".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.setZoom(ZoomKind.OUT);
			else if("fit".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.setZoom(ZoomKind.FIT);
			else if("full".equalsIgnoreCase(level)) //$NON-NLS-1$
				waveformViewerPart.setZoom(ZoomKind.FULL);
		}

	}
	
	
}