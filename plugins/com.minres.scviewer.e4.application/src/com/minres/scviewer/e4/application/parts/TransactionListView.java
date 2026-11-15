/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.minres.scviewer.database.ui.WaveformSelection;

public class TransactionListView {
	private TransactionList transactionList;

	/**
	 * Creates the composite.
	 *
	 * @param parent the parent
	 */
	@PostConstruct
	public void createComposite(final Composite parent, @Optional WaveformViewer waveformViewer) {
		transactionList = new TransactionList(parent, SWT.BORDER, waveformViewer);
	}
	
	@Focus
	public void setFocus() {
		if(transactionList!=null)
			transactionList.setFocus();
	}
	/**
	 * Receives selections directly from the owning waveform pane, independently of
	 * the workbench's active part and global selection injection.
	 *
	 * @param selection the new selection
	 */
	public void setSelection(IStructuredSelection selection) {
		if (transactionList != null && !transactionList.isDisposed())
			transactionList.setInput(WaveformSelection.from(selection).track());
	}

	public TransactionList getControl() {
		return transactionList;
	}

}
