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

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.e4.application.parts.DesignBrowser;

public class AddWaveformHandler {

	public static final String PARAM_WHERE_ID="com.minres.scviewer.e4.application.command.addwaveform.where"; //$NON-NLS-1$
	public static final String PARAM_ALL_ID="com.minres.scviewer.e4.application.command.addwaveform.all"; //$NON-NLS-1$
	
	@Inject @Optional DesignBrowser designBrowser;
	
	@CanExecute
	public boolean canExecute(@Named(PARAM_WHERE_ID) String where, @Named(PARAM_ALL_ID) String all,
			EPartService partService,
			@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection) {
		if(designBrowser==null) designBrowser = getListPart( partService);
		if(designBrowser==null || designBrowser.getActiveWaveformViewerPart()==null) return false;
		boolean before = "before".equalsIgnoreCase(where); //$NON-NLS-1$
		IStructuredSelection waveformSelection = null;
		if(designBrowser.getActiveWaveformViewerPart()!=null) {
			if(!designBrowser.getActiveWaveformViewerPart().getDatabase().isLoaded())
				return false;
			waveformSelection = (IStructuredSelection)designBrowser.getActiveWaveformViewerPart().getSelection();
		}
		if("true".equalsIgnoreCase(all))  //$NON-NLS-1$
			return designBrowser.getFilteredChildren().length>0 && 
					(!before || (waveformSelection!=null && waveformSelection.size()>0));
		else
			return selection!=null && selection.size()>0 && 
					(!before || (waveformSelection!=null && waveformSelection.size()>0));
	}

	@Execute
	public void execute(@Named(PARAM_WHERE_ID) String where, @Named(PARAM_ALL_ID) String all, 
			EPartService partService,
			@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection) {
		if(designBrowser==null) designBrowser = getListPart( partService);
		if(designBrowser!=null && selection.size()>0){
			@SuppressWarnings("unchecked")
			IWaveform[] sel=(IWaveform[]) selection.toList().stream().filter(t -> t instanceof IWaveform).toArray(IWaveform[]::new);
			designBrowser.getActiveWaveformViewerPart().addStreamsToList(sel, "before".equalsIgnoreCase(where)); //$NON-NLS-1$
		}
	}

	protected DesignBrowser getListPart(EPartService partService){
		MPart part = partService.getActivePart();
		if(part.getObject() instanceof DesignBrowser)
			return (DesignBrowser) part.getObject();
		else
			return null;
	}	
}
