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

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MDialog;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;

public class AboutHandler {

	static final String DIALOG_ID="com.minres.scviewer.e4.application.dialog.aboutscviewer";
	
	@Execute
	public void execute(Shell shell, MApplication app, MWindow window, EModelService ms /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
		MDialog dialog = (MDialog) ms.find(DIALOG_ID, app); //$NON-NLS-1$
		dialog.setToBeRendered(true);
		dialog.setToBeRendered(false);
	}

}
