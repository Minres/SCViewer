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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import com.minres.scviewer.e4.application.parts.DesignBrowser;

public class SelectAllHandler {

	@Inject @Optional DesignBrowser designBrowser;

	@Execute
	public void execute(EPartService partService) {
		if(designBrowser==null) designBrowser = getListPart(partService);
		if(designBrowser!=null){
			designBrowser.selectAllWaveforms();
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