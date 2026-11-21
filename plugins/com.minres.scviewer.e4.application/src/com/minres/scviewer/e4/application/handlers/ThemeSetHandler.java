/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

/*
 * see http://www.vogella.com/tutorials/Eclipse4CSS/article.html#tutorial_cssstyling
 */
@SuppressWarnings("restriction")
public class ThemeSetHandler {
	static final String PARAMTER_ID = "com.minres.scviewer.e4.application.command.theme.parameter.id"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(EPartService partService) {
		return true;
	}
	
	@Execute
	public void setTheme(@Named(PARAMTER_ID) String param, IThemeEngine engine) {
		if (!engine.getActiveTheme().getId().equals(param)) {
			// second argument defines that change is
			// persisted and restored on restart
			engine.setTheme(param, true);
		}
	}
}