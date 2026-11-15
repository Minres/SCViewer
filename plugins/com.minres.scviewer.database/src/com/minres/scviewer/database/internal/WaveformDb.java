/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
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
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

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
	private static final CopyOnWriteArrayList<IWaveformDbLoaderFactory> loaderFactories = new CopyOnWriteArrayList<>();

	private List<IWaveformDbLoader> activeLoader = new ArrayList<>();
	/** The loaded. */
	private volatile boolean loaded;

	/** The relation types. */
	private List<RelationType> relationTypes = new ArrayList<>();;

	/** The waveforms. */
	private Map<String, IWaveform> waveforms = Collections.synchronizedMap(new HashMap<>()) ;;

	/** The max time. */
	private volatile long maxTime = 0L;
	
	private static final Logger LOG = LoggerFactory.getLogger(WaveformDb.class);
	
	/**
	 * Bind.
	 *
	 * @param loader the loader
	 */
	public synchronized void bind(IWaveformDbLoaderFactory loader) {
		loaderFactories.addIfAbsent(loader);
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
		return List.copyOf(loaderFactories);
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
		synchronized (waveforms) { return new ArrayList<>(waveforms.values()); }
	}

	/**
	 * Load.
	 *
	 * @param inp the inp
	 * @return true, if successful
	 */
	@Override
	public boolean load(File inp) {
        List<PropertyChangeEvent> notifications = new ArrayList<>();
        boolean result;
        synchronized (this) {
            result = loadLocked(inp, notifications);
        }
        // A listener may wait for the UI, which in turn may need close().
        // Never invoke external listeners while holding the database lock.
        notifications.forEach(pcs::firePropertyChange);
        return result;
    }

    private boolean loadLocked(File inp, List<PropertyChangeEvent> notifications) {
        for (IWaveformDbLoaderFactory factory : getLoaders()) {
            if (!factory.canLoad(inp)) continue;
            IWaveformDbLoader loader = factory.getLoader();
            List<PropertyChangeEvent> pending = Collections.synchronizedList(new ArrayList<>());
            PropertyChangeListener collector = pending::add;
            long endTime;
            List<RelationType> types;
            loader.addPropertyChangeListener(collector);
            try {
                loader.load(inp);
                endTime = loader.getMaxTime();
                types = new ArrayList<>(loader.getAllRelationTypes());
            } catch (Exception e) {
                LOG.error("Error loading file " + inp.getName(), e);
                try { loader.dispose(); }
                catch (RuntimeException cleanup) { LOG.warn("Cannot dispose failed loader", cleanup); }
                continue;
            } finally {
                loader.removePropertyChangeListener(collector);
            }
            // Commit only completed loads; failed loaders never publish partial hierarchies.
            activeLoader.add(loader);
            maxTime = Math.max(maxTime, endTime);
            if (name == null) name = getFileBasename(inp.getName());
            for (RelationType type : types) if (!relationTypes.contains(type)) relationTypes.add(type);
            synchronized (pending) {
                for (PropertyChangeEvent event : pending) applyWaveform(event);
                if (!pending.isEmpty()) addWaveformNotifications(notifications);
            }
            loaded = true;
            notifications.add(new PropertyChangeEvent(this, IHierNode.LOADING_FINISHED, null, Boolean.TRUE));
            return true;
        }
        notifications.add(new PropertyChangeEvent(this, IHierNode.LOADING_FINISHED, null, Boolean.FALSE));
        return false;
    }

    @Override
    public synchronized void close() {
        for (IWaveformDbLoader loader : activeLoader) {
            try { loader.dispose(); }
            catch (RuntimeException e) { LOG.warn("Cannot dispose database loader", e); }
        }
        activeLoader.clear();
        waveforms.clear();
        childNodes.clear();
        relationTypes.clear();
        maxTime = 0;
        loaded = false;
        name = null;
    }

	/**
	 * Gets the file basename.
	 *
	 * @param f the f
	 * @return the file basename
	 */
	protected static String getFileBasename(String f) {
		String ext = f;
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
        List<PropertyChangeEvent> notifications = new ArrayList<>();
        synchronized (this) {
            if (applyWaveform(evt)) addWaveformNotifications(notifications);
        }
        notifications.forEach(pcs::firePropertyChange);
    }

    private boolean applyWaveform(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
        if (!IWaveformDbLoader.SIGNAL_ADDED.equals(property)
                && !IWaveformDbLoader.STREAM_ADDED.equals(property)
                && !IWaveformDbLoader.GENERATOR_ADDED.equals(property)) return false;
        IWaveform waveform = (IWaveform) evt.getNewValue();
        waveforms.put(waveform.getFullName(), waveform);
        if (!IWaveformDbLoader.GENERATOR_ADDED.equals(property)) putInHierarchy(waveform);
        return true;
    }

    private void addWaveformNotifications(List<PropertyChangeEvent> notifications) {
        notifications.add(new PropertyChangeEvent(this, IHierNode.WAVEFORMS, null, new HashMap<>(waveforms)));
        notifications.add(new PropertyChangeEvent(this, IHierNode.CHILDS, null, new ArrayList<>(childNodes)));
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
