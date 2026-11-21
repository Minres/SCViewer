/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;

public class AboutHandler {

	static final String DIALOG_ID="com.minres.scviewer.e4.application.dialog.aboutscviewer";
	
	@Execute
	public void execute(Shell shell, MApplication app, MWindow window, EModelService ms /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
		MPart mel = (MPart) ms.find(DIALOG_ID, app); //$NON-NLS-1$
		mel.setToBeRendered(true);
		mel.setToBeRendered(false);
	}

}
