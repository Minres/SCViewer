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

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class DeleteWaveformHandler {
	
	@SuppressWarnings("unchecked")
	@CanExecute
	public Boolean canExecute(ESelectionService selectionService){
		Object o = selectionService.getSelection();
		if(o instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) o;
			if(sel.size()>0)
				return sel.toList().stream().allMatch(e-> e instanceof TrackEntry);
			else
				return false;
		} else
			return false;
	}
	
	@Execute
	public void execute(ESelectionService selectionService, MPart activePart) {
		Object o = activePart.getObject();
		if(o instanceof WaveformViewer){
			((WaveformViewer)o).removeSelectedStreamsFromList();
		}	
	}	
}