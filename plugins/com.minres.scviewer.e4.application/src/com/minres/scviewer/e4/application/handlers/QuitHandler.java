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
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.e4.application.Messages;


public class QuitHandler {
	@Execute
	public void execute(IWorkbench workbench, Shell shell){
		if (MessageDialog.openConfirm(shell, Messages.QuitHandler_0,
				Messages.QuitHandler_1)) {
			workbench.close();
		}
	}
}
