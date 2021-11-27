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
package com.minres.scviewer.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.minres.scviewer.database.ui.ZoomKind;
import com.minres.scviewer.ui.TxEditorPart;

public class Zoom extends AbstractHandler {
	private static final String ZOOMIN_ID   = "com.minres.scviewer.ui.zoom.in";
	private static final String ZOOMOUT_ID  = "com.minres.scviewer.ui.zoom.out";
	private static final String ZOOMFIT_ID  = "com.minres.scviewer.ui.zoom.fit";
	private static final String ZOOMFULL_ID = "com.minres.scviewer.ui.zoom.full";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if(editor instanceof TxEditorPart){
		    String id = event.getCommand().getId();
			TxEditorPart txEditor=(TxEditorPart) editor;
		    if (ZOOMIN_ID.compareTo(id) == 0)
		    	txEditor.setZoom(ZoomKind.IN);
		    else if(ZOOMOUT_ID.compareTo(id) == 0)
		    	txEditor.setZoom(ZoomKind.OUT);
		    else if(ZOOMFIT_ID.compareTo(id) == 0)
		    	txEditor.setZoom(ZoomKind.FIT);
		    else if(ZOOMFULL_ID.compareTo(id) == 0)
		    	txEditor.setZoom(ZoomKind.FULL);
		}
		return null;
	}

}
