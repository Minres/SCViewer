/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.ui.views.provider;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IHierNode;

public class TxDbTreeContentProvider implements ITreeContentProvider {

	private IWaveformDb database;
	
	@Override
	public void dispose() {	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		database=(IWaveformDb)newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return database.getChildNodes().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof IHierNode){
			return ((IHierNode)parentElement).getChildNodes().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		Object[] obj = getChildren(element);
	    return obj == null ? false : obj.length > 0;
	}

}
