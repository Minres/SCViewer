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

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;
public class SearchHandler {

	@Evaluate
	@CanExecute
	public boolean evaluate(MPart activePart) {
		if (activePart == null || !(activePart.getObject() instanceof WaveformViewer viewer)) return false;
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection structured) {
			for (Object element : structured.toArray())
				if (element instanceof TrackEntry track && track.waveform != null
						&& track.waveform.getType().isTransaction()) return true;
		}
		return false;
	}

	@Execute
	public void execute(Shell shell, MPart activePart) {
		if (evaluate(activePart)) ((WaveformViewer) activePart.getObject()).focusTransactionSearch();
	}
}
