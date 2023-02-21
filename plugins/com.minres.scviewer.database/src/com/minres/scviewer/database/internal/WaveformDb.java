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
package com.minres.scviewer.database.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformDbLoaderFactory;
import com.minres.scviewer.database.RelationType;

/**
 * The Class WaveformDb.
 */
public class WaveformDb extends HierNode implements IWaveformDb, PropertyChangeListener {

	/** The loaders. */
	private static List<IWaveformDbLoaderFactory> loaderFactories = new LinkedList<>();

	/** The loaded. */
	private boolean loaded;

	/** The relation types. */
	private List<RelationType> relationTypes;

	/** The waveforms. */
	private Map<String, IWaveform> waveforms;

	/** The max time. */
	private long maxTime = -1;
	
	private static final Logger LOG = LoggerFactory.getLogger(WaveformDb.class);
	
	/**
	 * Bind.
	 *
	 * @param loader the loader
	 */
	public synchronized void bind(IWaveformDbLoaderFactory loader) {
		loaderFactories.add(loader);
	}

	/**
	 * Unbind.
	 *
	 * @param loader the loader
	 */
	public synchronized void unbind(IWaveformDbLoaderFactory loader) {
		loaderFactories.remove(loader);
	}

	/**
	 * Gets the loaders.
	 *
	 * @return the loaders
	 */
	public static List<IWaveformDbLoaderFactory> getLoaders() {
		return Collections.unmodifiableList(loaderFactories);
	}

	/**
	 * Instantiates a new waveform db.
	 */
	public WaveformDb() {
		super();
		waveforms = new ConcurrentHashMap<>();
		relationTypes = new ArrayList<>();
		maxTime = 0L;
	}

	/**
	 * Gets the max time.
	 *
	 * @return the max time
	 */
	@Override
	public long getMaxTime() {
		return maxTime;
	}

	/**
	 * Gets the stream by name.
	 *
	 * @param name the name
	 * @return the stream by name
	 */
	@Override
	public IWaveform getStreamByName(String name) {
		return waveforms.get(name);
	}

	/**
	 * Gets the all waves.
	 *
	 * @return the all waves
	 */
	@Override
	public List<IWaveform> getAllWaves() {
		return new ArrayList<>(waveforms.values());
	}

	/**
	 * Load.
	 *
	 * @param inp the inp
	 * @return true, if successful
	 */
	@Override
	public boolean load(File inp) {
		boolean retval = true;
		for (IWaveformDbLoaderFactory loaderFactory : loaderFactories) {
			if (loaderFactory.canLoad(inp)) {
				IWaveformDbLoader loader = loaderFactory.getLoader();
				loader.addPropertyChangeListener(this);
				try {
					loader.load(this, inp);
				} catch (Exception e) {
					LOG.error("error loading file "+inp.getName(), e);
					retval=false;
				}
				loader.removePropertyChangeListener(this);
				for (IWaveform w : loader.getAllWaves()) {
					waveforms.put(w.getFullName(), w);
				}
				if (loader.getMaxTime() > maxTime) {
					maxTime = loader.getMaxTime();
				}
				if (name == null)
					name = getFileBasename(inp.getName());
				buildHierarchyNodes();
				relationTypes.addAll(loader.getAllRelationTypes());
			}
		}
		pcs.firePropertyChange(IHierNode.LOADING_FINISHED, null, null);
		loaded = true;
		return retval;
	}

	/**
	 * Gets the file basename.
	 *
	 * @param f the f
	 * @return the file basename
	 */
	protected static String getFileBasename(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');
		if (i > 0 && i < f.length() - 1) {
			ext = f.substring(0, i);
		}
		return ext;
	}

	/**
	 * Checks if is loaded.
	 *
	 * @return true, if is loaded
	 */
	public boolean isLoaded() {
		return loaded;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (IWaveformDbLoader.SIGNAL_ADDED.equals(evt.getPropertyName())
				|| IWaveformDbLoader.STREAM_ADDED.equals(evt.getPropertyName())) {
			IWaveform waveform = (IWaveform) evt.getNewValue();
			waveforms.put(waveform.getFullName(), waveform);
			putInHierarchy(waveform);
			pcs.firePropertyChange(IHierNode.WAVEFORMS, null, waveforms);
			pcs.firePropertyChange(IHierNode.CHILDS, null, childNodes);
		} else if (IWaveformDbLoader.GENERATOR_ADDED.equals(evt.getPropertyName())) {
			pcs.firePropertyChange(IHierNode.CHILDS, null, childNodes);
		}
	}

	/**
	 * Builds the hierarchy nodes.
	 */
	private synchronized void buildHierarchyNodes() {
		boolean needsSorting = false;
		for (IWaveform stream : getAllWaves()) {
			if (stream.getParent() == null) {
				putInHierarchy(stream);
				needsSorting = true;
			}
		}
		if (needsSorting) {
			pcs.firePropertyChange(IHierNode.WAVEFORMS, null, waveforms);
			pcs.firePropertyChange(IHierNode.CHILDS, null, childNodes);
		}
	}

	private synchronized void putInHierarchy(IWaveform waveform) {
		String[] hier = waveform.getName().split("\\.");
		IHierNode node = this;
		for (int i = 0; i < hier.length - 1; ++i) {
			String name = hier[i];
			IHierNode childNode = null;
			for (IHierNode n : node.getChildNodes()) {
				if (n.getName().equals(name)) {
					childNode = n;
					break;
				}
			}
			if (childNode == null) {
				HierNode newNode = new HierNode(name, node);
				node.addChild(newNode);
				node = newNode;
			} else {
				node = childNode;
			}
		}
		node.addChild(waveform);
		waveform.setParent(node);
		waveform.setName(hier[hier.length - 1]);
	}

	/**
	 * Gets the all relation types.
	 *
	 * @return the all relation types
	 */
	@Override
	public List<RelationType> getAllRelationTypes() {
		return relationTypes;
	}

}
