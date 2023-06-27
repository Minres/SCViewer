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
package com.minres.scviewer.database.fst;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.google.common.collect.Iterables;
import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DoubleVal;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;

/**
 * The Class VCDDb.
 */
public class FstDbLoader implements IWaveformDbLoader, IFstDatabaseBuilder {


	/** The module stack. */
	private ArrayDeque<String> moduleStack;

	/** The signals. */
	private List<IWaveform> signals;

	FstFileParser parser;
	/** The max time. */
	private long maxTime;

	private long timeScaleFactor;
	
	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    static long calculateTimescaleMultipierPower(long time_scale){
    	long answer = 1;
        if(time_scale<=0){
            return answer;
        } else{
            for(int i = 1; i<= time_scale; i++)
                answer *= 10;
            return answer;
        }
    }

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.ITrDb#load(java.io.File)
	 */
	@Override
	public void load(File file) throws InputFormatException {
		dispose();
		this.maxTime=0;
		boolean res = false;
		signals = new Vector<>();
		moduleStack= new ArrayDeque<>();
		parser = new FstFileParser(file);
		res = parser.open(this);
		moduleStack=null;
		if(!res) 
			throw new InputFormatException("Could not parse VCD file");
		// calculate max time of this database
		pcs.firePropertyChange(IWaveformDbLoader.LOADING_FINISHED, null, null);
	}

	public void dispose() {
		if(parser!=null) {
			parser.close();
			parser=null;
		}
		moduleStack=null;
		signals=null;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.ITrDb#getMaxTime()
	 */
	@Override
	public long getMaxTime() {
		return maxTime;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.ITrDb#getAllWaves()
	 */
	@Override
	public Collection<IWaveform> getAllWaves() {
		return signals;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#enterModule(java.lang.String)
	 */
	@Override
	public void enterModule(String tokenString) {
		if(moduleStack.isEmpty()) {
			if("SystemC".compareTo(tokenString)!=0)
				moduleStack.push(tokenString);
		} else
			moduleStack.push(moduleStack.peek()+"."+tokenString);

	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#exitModule()
	 */
	@Override
	public void exitModule() {
		if(!moduleStack.isEmpty()) moduleStack.pop();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#newNet(java.lang.String, int, int)
	 */
	@Override
	public void newNet(String name, int handle, int width, int direction, boolean alias) {
		String netName = moduleStack.isEmpty()? name: moduleStack.peek()+"."+name;
		IWaveform signal = width==0?
				new FstSignal<DoubleVal>(this, handle, netName, width, direction):
					new FstSignal<BitVector>(this, handle, netName, direction, width);
		signals.add(signal);
		pcs.firePropertyChange(IWaveformDbLoader.SIGNAL_ADDED, null, Iterables.getLast(signals));
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#getNetWidth(int)
	 */
	@Override
	public int getNetWidth(int intValue) {
		FstSignal<?> signal = (FstSignal<?>) signals.get(intValue);
		return signal.getRowCount();
	}

	public void setMaxTime(long maxTime, int timeScale) {
		if(timeScale>0) timeScale=-timeScale;
		long eff_time_scale=timeScale-IWaveformDb.databaseTimeScale;
		this.timeScaleFactor = calculateTimescaleMultipierPower(eff_time_scale);
		this.maxTime = maxTime*timeScaleFactor;
	}	
	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.IWaveformDbLoader#getAllRelationTypes()
	 */
	@Override
	public Collection<RelationType> getAllRelationTypes(){
		return Collections.emptyList();
	}

	/**
	 * Adds the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * Removes the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	public void getEvents(int id, int width, IEventList values) {
		if(values instanceof EventList)
			parser.getValueChanges(id, width, timeScaleFactor, (EventList) values);
	}
}
