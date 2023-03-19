/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
 
package com.minres.scviewer.e4.application.handlers;

import java.util.Optional;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.HierState;
import com.minres.scviewer.e4.application.parts.TextInputDialog;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class WaveformDoubleClickTextHandler {

	@Execute
	public void execute(Shell shell, EPartService partService) {
		MPart part = partService.getActivePart();
		if(part!=null && part.getObject() instanceof WaveformViewer){
			Object sel = ((WaveformViewer)part.getObject()).getSelection();
			if( sel instanceof IStructuredSelection) {
				@SuppressWarnings("unchecked")
				Optional<TrackEntry> o = ((IStructuredSelection)sel).toList().stream().filter(e -> e instanceof TrackEntry).findFirst();
				if(o.isPresent()) {
					TrackEntry te = o.get();
					if(te.waveform instanceof EmptyWaveform) {
						EmptyWaveform waveform= (EmptyWaveform)te.waveform;
						TextInputDialog dialog = new TextInputDialog(shell);
						dialog.create();
						dialog.setTitle("Change Separator Text");
						dialog.setText(waveform.getName());
						if (dialog.open() == Window.OK) {
							waveform.setName(dialog.getText());
						}
					} else if(te.hierState==HierState.CLOSED) {
						te.hierState=HierState.OPENED;
					} else if(te.hierState==HierState.OPENED) {
						te.hierState=HierState.CLOSED;
					}
				}
			}
		}
	}
	
	
	@Evaluate
	@CanExecute
	public Boolean canExecute(MPart activePart){
		if(activePart!=null && activePart.getObject() instanceof WaveformViewer){
			Object sel = ((WaveformViewer)activePart.getObject()).getSelection();
			if( sel instanceof IStructuredSelection) {
				if(((IStructuredSelection)sel).isEmpty()) return false;
				@SuppressWarnings("unchecked")
				Optional<TrackEntry> o = ((IStructuredSelection)sel).toList().stream().filter(e -> e instanceof TrackEntry).findFirst();
				if(o.isPresent()) {
					TrackEntry te = o.get();
					return te.waveform instanceof EmptyWaveform || te.hierState!=HierState.NONE;
				}
			}
		}
		return false;
	}
		
}