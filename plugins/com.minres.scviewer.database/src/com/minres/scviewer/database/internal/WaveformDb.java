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
package com.minres.scviewer.database.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.RelationType;

public class WaveformDb extends HierNode implements IWaveformDb {

	private static List<IWaveformDbLoader> loaders=new LinkedList<>();

	private boolean loaded;

	private List<RelationType> relationTypes;
	
	private Map<String, IWaveform> waveforms;

	private Long maxTime;
	
	
	public synchronized void bind(IWaveformDbLoader loader){
		loaders.add(loader);
	}

	public synchronized void unbind(IWaveformDbLoader loader){
		loaders.remove(loader);
	}

	
	public static List<IWaveformDbLoader> getLoaders() {
		return Collections.unmodifiableList(loaders);
	}

	public WaveformDb() {
		super();
		waveforms = new HashMap<>();
		relationTypes=new ArrayList<>();
		maxTime=0L;
	}

	@Override
	public Long getMaxTime() {
		return maxTime;
	}

	@Override
	public IWaveform getStreamByName(String name) {
		return waveforms.get(name);
	}

	@Override
	public List<IWaveform> getAllWaves() {
		return new ArrayList<>(waveforms.values());
	}

	@Override
	public boolean load(File inp){
		for(IWaveformDbLoader loader:loaders){
			try {
				if(loader.load(this, inp)){
					for(IWaveform w:loader.getAllWaves()){
						waveforms.put(w.getFullName(),w);
					}
					if(loader.getMaxTime()>maxTime){
						maxTime=loader.getMaxTime();
					}
					if(name==null) name=getFileBasename(inp.getName());
					buildHierarchyNodes() ;
					relationTypes.addAll(loader.getAllRelationTypes());
					pcs.firePropertyChange("WAVEFORMS", null, waveforms);
					pcs.firePropertyChange("CHILDS", null, childs);
					loaded = true;
					return true;
				}
			} catch (Exception e) {
				return false;
			}
		}		
		return false;
	}

	protected static String getFileBasename(String f) {
	    String ext = "";
	    int i = f.lastIndexOf('.');
	    if (i > 0 &&  i < f.length() - 1) {
	      ext = f.substring(0, i);
	    }
	    return ext;
	  }

	@Override
	public void clear() {
		waveforms.clear();
		childs.clear();
		loaded=false;
	}

	public boolean isLoaded() {
		return loaded;
	}

	private void buildHierarchyNodes() {
		for(IWaveform stream:getAllWaves()){
			String[] hier = stream.getName().split("\\.");
			IHierNode node = this;
			for(int i=0; i<hier.length-1; ++i){
				String name = hier[i];
				IHierNode childNode = null;
				for (IHierNode n : node.getChildNodes()) {
					if (n.getName().equals(name)) {
						childNode=n;
						break;
					}
				}
				if(childNode != null) {
					node = childNode;
					break;
				}
				HierNode newNode = new HierNode(name, node);
				node.getChildNodes().add(newNode);
				node=newNode;
					
			}
			node.getChildNodes().add(stream);
			stream.setParent(node);
			stream.setName(hier[hier.length-1]);
		}
		sortRecursive(this);
	}

	private void sortRecursive(IHierNode node) {
		Collections.sort(node.getChildNodes(), (IHierNode o1, IHierNode o2) -> o1.getName().compareTo(o2.getName()));
		for(IHierNode n:node.getChildNodes()) {
			if(!n.getChildNodes().isEmpty())
				sortRecursive(n);
		}
	}

	@Override
	public List<RelationType> getAllRelationTypes() {
		return relationTypes;
	}
}
