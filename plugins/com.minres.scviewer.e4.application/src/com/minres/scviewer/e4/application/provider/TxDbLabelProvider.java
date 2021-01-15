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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.swt.ResourceManager;

import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.e4.application.Constants;

/**
 * The Class TxDbLabelProvider providing the labels for the respective viewers.
 */
public class TxDbLabelProvider implements ILabelProvider {

	/** The listeners. */
	private List<ILabelProviderListener> listeners = new ArrayList<>();

	/** The wave. */
	private Image loadinDatabase;
	
	/** The database. */
	private Image database;
	
	/** The stream. */
	private Image stream;
	
	/** The signal. */
	private Image signal;
	
	/** The folder. */
	private Image folder;
	
	/** The wave. */
	private Image wave;
	
	/**
	 * Instantiates a new tx db label provider.
	 */
	public TxDbLabelProvider(boolean isTree) {
		super();
		loadinDatabase=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/database_go.png"); //$NON-NLS-1$ //$NON-NLS-2$
		database=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/database.png"); //$NON-NLS-1$ //$NON-NLS-2$
		folder=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
		if(isTree) {
			stream=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/stream_hier.png"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			stream=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/stream.png"); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		signal=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/signal.png"); //$NON-NLS-1$ //$NON-NLS-2$
		wave=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/wave.png"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Adds the listener.
	 *
	 * @param listener the listener
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		  listeners.add(listener);
	}

	/**
	 * Dispose.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		// no resources to dispose
	}

	/**
	 * Checks if is label property.
	 *
	 * @param element the element
	 * @param property the property
	 * @return true, if is label property
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		  return false;
	}

	/**
	 * Removes the listener.
	 *
	 * @param listener the listener
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		  listeners.remove(listener);
	}

	/**
	 * Gets the image.
	 *
	 * @param element the element
	 * @return the image
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if(element instanceof IWaveformDb){
			return ((IWaveformDb)element).isLoaded()?database:loadinDatabase;
		}else if(element instanceof IWaveform){
			switch(((IWaveform) element).getType()) {
			case TRANSACTION:
				return stream;
			case FILTER:
				break;
			case SIGNAL:
				if(((IWaveform) element).getRowCount()==1)
					return signal;
				else 
					return wave;
			default:
				break;
			}
			return wave;
		}else if(element instanceof IHierNode){
			return folder;
		} else
			return null;
	}

	/**
	 * Gets the text.
	 *
	 * @param element the element
	 * @return the text
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if(element instanceof IWaveformDb){
			IWaveformDb db = (IWaveformDb) element;
			if(db.getName()== null)
				return "";
			return db.getName()+(db.isLoaded()?"":" (loading)");
		} else
			return ((IHierNode)element).getName();
	}

}


