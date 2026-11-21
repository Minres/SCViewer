/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class HelpHandler {

	static final String WINDOW_ID="com.minres.scviewer.e4.application.window.web_help"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(MApplication app) {
		return !app.getChildren().stream().filter(e -> e.getElementId().equals(WINDOW_ID)).findFirst().isPresent();
	}
	
	@Execute
	public void execute(MApplication app, EModelService modelService /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
		MWindow newWin = (MWindow)modelService.cloneSnippet(app, WINDOW_ID, null);
		final IEclipseContext ctx=app.getContext();
		if(ctx.containsKey("help_url"))
			ctx.remove("help_url");
		ctx.modify("help_url", "https://minres.github.io/SCViewer/#key-shortcuts");
		app.getChildren().add(newWin);
	}
}
