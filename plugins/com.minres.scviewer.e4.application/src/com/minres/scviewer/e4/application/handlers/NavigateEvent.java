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
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class NavigateEvent {

	final static String PARAMTER_ID="com.minres.scviewer.e4.application.command.navigateEventCommand.parameter.dir"; //$NON-NLS-1$

	@CanExecute
	public Boolean canExecute(EPartService partService){
		MPart part = partService.getActivePart();
		if(part.getObject() instanceof WaveformViewer){
			Object sel = ((WaveformViewer)part.getObject()).getSelection();
			if( sel instanceof IStructuredSelection) {
				if(((IStructuredSelection)sel).isEmpty()) return false;
				Object o= ((IStructuredSelection)sel).getFirstElement();
				return o instanceof IWaveform || o instanceof ITx || o instanceof TrackEntry;
			}
		}
		return false;
	}

	@Execute
	public void execute(@Named(PARAMTER_ID) String param, EPartService partService) {
		//	public void execute(EPartService partService) {
		//		String param="next";
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		if(obj instanceof WaveformViewer){
			if("next".equalsIgnoreCase(param)) //$NON-NLS-1$
				((WaveformViewer)obj).moveCursor(GotoDirection.NEXT);
			else if("prev".equalsIgnoreCase(param)) //$NON-NLS-1$
				((WaveformViewer)obj).moveCursor(GotoDirection.PREV);
		}
	}
}