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
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class MoveWaveformHandler {

	static final String PARAMETER_ID="com.minres.scviewer.e4.application.command.movewaveformupCommand.parameter.dir"; //$NON-NLS-1$

	@CanExecute
	public Boolean canExecute(EPartService partService){
		MPart part = partService.getActivePart();
		if(part!=null && part.getObject() instanceof WaveformViewer){
			Object sel = ((WaveformViewer)part.getObject()).getSelection();
			if( sel instanceof IStructuredSelection) {
				if(((IStructuredSelection)sel).isEmpty()) return false;
				Object o= ((IStructuredSelection)sel).getFirstElement();
				return o instanceof TrackEntry;
			}
		}
		return false;
	}
	
	@Execute
	public void execute(@Named(PARAMETER_ID) String param, EPartService partService) {
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		if(obj instanceof WaveformViewer){
			if("up".equalsIgnoreCase(param)) //$NON-NLS-1$
				((WaveformViewer)obj).moveSelected(-1);
			else if("down".equalsIgnoreCase(param)) //$NON-NLS-1$
				((WaveformViewer)obj).moveSelected(1);
		}
	}
		
}