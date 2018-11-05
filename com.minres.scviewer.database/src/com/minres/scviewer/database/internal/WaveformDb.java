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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformEvent;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;

public class WaveformDb extends HierNode implements IWaveformDb {

	private static List<IWaveformDbLoader> loaders=new LinkedList<IWaveformDbLoader>();

	private boolean loaded;

	private List<IHierNode> childNodes;

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
		waveforms = new HashMap<String, IWaveform>();
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
		return new ArrayList<IWaveform>(waveforms.values());
	}

	@Override
	public boolean load(File inp) throws Exception {
		for(IWaveformDbLoader loader:loaders){
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
				pcs.firePropertyChange("CHILDS", null, childNodes);
				loaded = true;
				return true;
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
		childNodes.clear();
		loaded=false;
	}

	public boolean isLoaded() {
		return loaded;
	}

	private void buildHierarchyNodes() throws InputFormatException{
		childNodes= new ArrayList<IHierNode>();
		for(IWaveform stream:getAllWaves()){
			//updateMaxTime(stream);
			String[] hier = stream.getName().split("\\.");
			IHierNode node = this;
			List<String> path=new LinkedList<String>();
			path.add(name);
			for(String name:hier){
				IHierNode n1 = null;
				for (IHierNode n : node.getChildNodes()) {
					if (n.getName().equals(name)) {
						n1=n;
						break;
					}
				}
				if(name == hier[hier.length-1]){ //leaf
					if(n1!=null) {
						if(n1 instanceof HierNode){
							node.getChildNodes().remove(n1);
							stream.getChildNodes().addAll(n1.getChildNodes());
							Collections.sort(stream.getChildNodes());
						} else {
							throw new InputFormatException();
						}
					}
					stream.setName(name);
					stream.setParentName(join(path, "."));
					node.getChildNodes().add(stream);
					Collections.sort(node.getChildNodes());
					node=stream;
				} else { // intermediate
					if(n1 != null) {
						node=n1;
					} else {
						HierNode newNode = new HierNode(name, join(path, "."));
						node.getChildNodes().add(newNode);
						Collections.sort(node.getChildNodes());
						node=newNode;
					}
				}
				path.add(name);
			}
		}
	}

	private static String join(Collection<?> col, String delim) {
	    StringBuilder sb = new StringBuilder();
	    Iterator<?> iter = col.iterator();
	    if (iter.hasNext())
	        sb.append(iter.next().toString());
	    while (iter.hasNext()) {
	        sb.append(delim);
	        sb.append(iter.next().toString());
	    }
	    return sb.toString();
	}

	@Override
	public List<RelationType> getAllRelationTypes() {
		return relationTypes;
	}
}
