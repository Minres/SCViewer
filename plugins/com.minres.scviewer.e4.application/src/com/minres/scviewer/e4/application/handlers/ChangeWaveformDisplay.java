 
package com.minres.scviewer.e4.application.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class ChangeWaveformDisplay {
	
	static final String PARAMETER_ID="com.minres.scviewer.e4.application.commandparameter.changewavedisplay"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(EPartService partService) {
		MPart part = partService.getActivePart();
		if(part==null) return false;
		return (part.getObject() instanceof WaveformViewer);
	}

	@Execute
	public void execute(@Named(PARAMETER_ID) String param, EPartService partService) {
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			ISelection sel = wfv.getSelection();
			if(!sel.isEmpty() && sel instanceof IStructuredSelection) {
				for(Object elem:(IStructuredSelection)sel) {
					if(elem instanceof TrackEntry) {
						TrackEntry.WaveDisplay val= TrackEntry.WaveDisplay.valueOf(param);
						((TrackEntry)elem).waveDisplay=val;
					}
				}
				wfv.update();
			}
		}
	}
		
}