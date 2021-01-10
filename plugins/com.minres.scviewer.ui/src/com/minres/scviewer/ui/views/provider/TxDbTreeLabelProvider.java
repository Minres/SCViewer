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
package com.minres.scviewer.ui.views.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.ui.TxEditorPlugin;

public class TxDbTreeLabelProvider implements ILabelProvider {

	private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	private Image database;
	private Image stream;
	private Image signal;
	private Image folder;
	private Image wave;

	
	public TxDbTreeLabelProvider() {
		super();
		database=TxEditorPlugin.createImage("database");
		stream=TxEditorPlugin.createImage("stream");
		folder=TxEditorPlugin.createImage("folder");
		signal=TxEditorPlugin.createImage("signal");
		wave=TxEditorPlugin.createImage("wave");

	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		  listeners.add(listener);
	}

	@Override
	public void dispose() {
		if(database!=null) database.dispose();
		if(stream!=null) stream.dispose();
		if(folder!=null) folder.dispose();
		if(signal!=null) signal.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		  return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		  listeners.remove(listener);
	}

	@Override
	public Image getImage(Object element) {
		if(element instanceof IWaveformDb){
			return database;
		}else if(element instanceof IWaveform){
			switch(((IWaveform) element).getType()) {
			case TRANSACTION:
				return stream;
			case FILTER:
				break;
			case SIGNAL:
				if(((IWaveform) element).getWidth()==1)
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

	@Override
	public String getText(Object element) {
		return ((IHierNode)element).getName();
	}

}


