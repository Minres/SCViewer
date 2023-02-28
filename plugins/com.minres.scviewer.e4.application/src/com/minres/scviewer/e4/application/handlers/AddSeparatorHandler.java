/*******************************************************************************
 * Copyright (c) 2023 MINRES Technologies GmbH and others.
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

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class AddSeparatorHandler {

	public static final String PARAM_WHERE_ID="com.minres.scviewer.e4.application.commandparameter.add_separator"; //$NON-NLS-1$
	
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
	public void execute(@Named(PARAM_WHERE_ID) String where, EPartService partService) {
		Object obj = partService.getActivePart().getObject();
		if(obj instanceof WaveformViewer){
			((WaveformViewer)obj).addStreamsToList(
					new IWaveform[]{new EmptyWaveform()}, "before".equalsIgnoreCase(where)); //$NON-NLS-1$
		}
	}

}
