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
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class HelpHandler {

	static final String WINDOW_ID="com.minres.scviewer.e4.application.window.help"; //$NON-NLS-1$
	@CanExecute
	public boolean canExecute(MApplication app) {
		return !app.getChildren().stream().filter(e -> e.getElementId().equals(WINDOW_ID)).findFirst().isPresent();
	}
	
	@Execute
	public void execute(MApplication app, MWindow window, EModelService modelService /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
		MWindow newWin = (MWindow)modelService.cloneSnippet(app, WINDOW_ID, null);
		app.getChildren().add(newWin);
	}

}
