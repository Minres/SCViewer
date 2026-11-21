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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class SetRelationTypeHandler {
	final static String PARAMTER_ID="com.minres.scviewer.e4.application.commandparameter.relationName"; //$NON-NLS-1$

	@Execute
	public void execute(@Named(PARAMTER_ID) String relationName, EPartService partService) {
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer waveformViewerPart = (WaveformViewer) obj;
			waveformViewerPart.setNavigationRelationType(relationName);
		}
	}
		
}