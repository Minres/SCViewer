/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.ui.views;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.PropertySheet;

public class TransactionPropertySheet extends PropertySheet {
	
	
	public TransactionPropertySheet() {
		super();
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part.getSite().getId().equals("com.minres.scviewer.ui.TxEditorPart");
	}
}
