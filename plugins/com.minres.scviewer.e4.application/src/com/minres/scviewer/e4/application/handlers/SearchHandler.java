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

import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;
public class SearchHandler {

	@Evaluate
	public boolean evaluate(MPart activePart) {
		Object obj = activePart.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			ISelection sel = wfv.getSelection();
			if(sel instanceof StructuredSelection) {
				Object[] elem = ((StructuredSelection)sel).toArray();
				if(elem.length ==0) return false;
				TrackEntry e = findTrackEntry(elem);
				return e!=null && e.isStream();
			}
		}
		return false;
	}

	@Execute
	public void execute(Shell shell, MPart activePart){
		Object obj = activePart.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			wfv.showSearch();
//			ISelection sel = wfv.getSelection();
//			if(sel instanceof StructuredSelection) {
//				TrackEntry e = findTrackEntry(((StructuredSelection)sel).toArray());
//				SearchTxDialog dlg = new SearchTxDialog(shell, e.getStream());
//				if (dlg.open() != Window.OK) return;
//				wfv.search(dlg.getPropName(), dlg.getPropType(), dlg.getPropValue());
//			}
		}
	}
	
	private TrackEntry findTrackEntry(Object[] elems) {
		for(Object o: elems)
			if(o instanceof TrackEntry)
				return (TrackEntry)o;
		return null;
	}
}
