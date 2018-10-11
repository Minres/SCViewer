 
package com.minres.scviewer.e4.application.elements;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.di.AboutToHide;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ISignalChange;
import com.minres.scviewer.database.ISignalChangeBit;
import com.minres.scviewer.database.ISignalChangeBitVector;
import com.minres.scviewer.database.ISignalChangeReal;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class WaveformPopupMenuContribution {
	int counter=0;
	
	@Inject MPart activePart;
	
	@AboutToShow
	public void aboutToShow(List<MMenuElement> items) {
		Object obj = activePart.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			ISelection sel = wfv.getSelection();
			if(!sel.isEmpty() && sel instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection)sel).getFirstElement();
				if(selected instanceof ISignal<?>) {
					ISignalChange s = (ISignalChange) ((ISignal<?>) selected).getEvents().firstEntry().getValue();
					if(s instanceof ISignalChangeReal) {
				        MDirectMenuItem mdi = MMenuFactory.INSTANCE.createDirectMenuItem();
				        mdi.setLabel("Analog linear");
						items.add(mdi);
				        mdi = MMenuFactory.INSTANCE.createDirectMenuItem();
				        mdi.setLabel("Analog step");
						items.add(mdi);
					} else if(s instanceof ISignalChangeBitVector) {
				        MDirectMenuItem mdi = MMenuFactory.INSTANCE.createDirectMenuItem();
				        mdi.setLabel("Analog linear");
						items.add(mdi);
				        mdi = MMenuFactory.INSTANCE.createDirectMenuItem();
				        mdi.setLabel("Analog step");
						items.add(mdi);
					}
				}
			}
		}

	}
	
	
	@AboutToHide
	public void aboutToHide(List<MMenuElement> items) {
		
	}
		
}