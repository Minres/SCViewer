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
package com.minres.scviewer.e4.application.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.swt.ResourceManager;

import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.e4.application.parts.LoadingWaveformDb;

/**
 * The Class TxDbLabelProvider providing the labels for the respective viewers.
 */
public class TxDbLabelProvider implements ILabelProvider {

	/** The listeners. */
	private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	/** The wave. */
	private Image loadinDatabase, database, stream, signal, folder, wave;
	
	
	/**
	 * Instantiates a new tx db label provider.
	 */
	public TxDbLabelProvider() {
		super();
		loadinDatabase=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/database_go.png"); //$NON-NLS-1$ //$NON-NLS-2$
		database=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/database.png"); //$NON-NLS-1$ //$NON-NLS-2$
		stream=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/stream.png"); //$NON-NLS-1$ //$NON-NLS-2$
		folder=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
		signal=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/signal.png"); //$NON-NLS-1$ //$NON-NLS-2$
		wave=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/wave.png"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		  listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		if(loadinDatabase!=null) database.dispose();
		if(database!=null) database.dispose();
		if(stream!=null) stream.dispose();
		if(folder!=null) folder.dispose();
		if(signal!=null) signal.dispose();
		if(wave!=null) wave.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		  return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		  listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if(element instanceof IWaveformDb){
			if(element instanceof LoadingWaveformDb)
				return loadinDatabase;
			else
				return database;
		}else if(element instanceof ITxStream){
			return stream;
		}else if(element instanceof ISignal<?>){
			Object o = ((ISignal<?>)element).getEvents().firstEntry().getValue();
			if(o instanceof BitVector && ((BitVector)o).getWidth()==1)
				return signal;
			else 
				return wave;
		}else if(element instanceof IHierNode){
			return folder;
		} else
			return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		return ((IHierNode)element).getName();
	}

}


