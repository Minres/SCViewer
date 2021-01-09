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
package com.minres.scviewer.e4.application.provider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;

import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;

/**
 * The Class TxDbContentProvider providing the tree content of a database for the respective viewer.
 */
public class TxDbContentProvider implements ITreeContentProvider {

	/** The show nodes. */
	private boolean tableEntries;

	/**
	 * Instantiates a new tx db content provider.
	 */
	public TxDbContentProvider() {
		super();
		this.tableEntries = false;
	}

	/**
	 * Instantiates a new tx db content provider.
	 *
	 * @param tableEntries get nodes for waveform table entries
	 */
	public TxDbContentProvider(boolean tableEntries) {
		super();
		this.tableEntries = tableEntries;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if(tableEntries && inputElement instanceof IWaveformDb){
			return new Object[]{};
		}else if(inputElement instanceof IHierNode){
			Collection<IHierNode> res = ((IHierNode)inputElement).getChildNodes().stream().filter(n ->
				tableEntries? n instanceof IWaveform : !n.getChildNodes().isEmpty()
			).collect(Collectors.toList());
			return res.toArray();
		}else if(inputElement instanceof List<?>){
			return ((List<?>)inputElement).toArray();
		} else
			return new Object[]{};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] obj = getElements(element);
		return obj.length > 0;
	}

}
