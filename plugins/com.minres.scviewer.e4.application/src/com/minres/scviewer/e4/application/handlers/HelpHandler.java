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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class HelpHandler {

	static final String DIALOG_ID="com.minres.scviewer.e4.application.dialog.onlinehelp"; //$NON-NLS-1$
	static final String WINDOW_ID="com.minres.scviewer.e4.application.window.help"; //$NON-NLS-1$
	
	@Execute
	public void execute(MApplication app, /*MWindow window,*/ EModelService ms /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
//		MPart mel = (MPart) ms.find(DIALOG_ID, app); //$NON-NLS-1$
//		mel.setToBeRendered(true);
//		mel.setToBeRendered(false);
		MUIElement w = ms.find(WINDOW_ID, app); 
		if(w!=null) w.setToBeRendered(true);
	}

}
