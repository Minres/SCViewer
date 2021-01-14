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
package com.minres.scviewer.database.vcd;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import com.google.common.collect.Iterables;
import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DoubleVal;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;

/**
 * The Class VCDDb.
 */
public class VCDDbLoader implements IWaveformDbLoader, IVCDDatabaseBuilder {


	/** The Constant TIME_RES. */
	private static final Long TIME_RES = 1000L; // ps

	/** The module stack. */
	private ArrayDeque<String> moduleStack;

	/** The signals. */
	private List<IWaveform> signals;

	/** The max time. */
	private long maxTime;

	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private static boolean isGzipped(File f) {
		try (InputStream is = new FileInputStream(f)) {
			byte [] signature = new byte[2];
			int nread = is.read( signature ); //read the gzip signature
			return nread == 2 && signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b;
		}
		catch (IOException e) {
			return false;
		}
	}


	/**
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		if(!inputFile.isDirectory() || inputFile.exists()) {
			String name = inputFile.getName();
			if(!(name.endsWith(".vcd") ||
					name.endsWith(".vcdz") ||
					name.endsWith(".vcdgz")  ||
					name.endsWith(".vcd.gz")) )
				return false;
			boolean gzipped = isGzipped(inputFile);
			try(InputStream stream = gzipped ? new GZIPInputStream(new FileInputStream(inputFile)) : new FileInputStream(inputFile)){
				byte[] buffer = new byte[8];
				if (stream.read(buffer, 0, buffer.length) == buffer.length) {
					return buffer[0]=='$';
				}
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.ITrDb#load(java.io.File)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void load(IWaveformDb db, File file) throws InputFormatException {
		dispose();
		this.maxTime=0;
		boolean res = false;
		try {
			signals = new Vector<>();
			moduleStack= new ArrayDeque<>();
			FileInputStream fis = new FileInputStream(file);
			res = new VCDFileParser(false).load(isGzipped(file)?new GZIPInputStream(fis):fis, this);
			moduleStack=null;
		} catch(IOException e) { 
			moduleStack=null;
			throw new InputFormatException(e.toString());
		}
		if(!res) throw new InputFormatException("Could not parse VCD file");
		// calculate max time of this database
		for(IWaveform waveform:signals) {
			NavigableMap<Long, IEvent[]> events =waveform.getEvents();
			if(!events.isEmpty())
				maxTime= Math.max(maxTime, events.lastKey());
		}
		// extend signals to have a last value set at max time
		for(IWaveform s:signals){
			if(s instanceof VCDSignal<?>) {
				TreeMap<Long,?> events = (TreeMap<Long, ?>) ((VCDSignal<?>)s).getEvents();
				if(events.size()>0 && events.lastKey()<maxTime){
					Object val = events.lastEntry().getValue();
					if(val instanceof BitVector) {
						((VCDSignal<BitVector>)s).addSignalChange(maxTime, (BitVector) val);
					} else if(val instanceof DoubleVal)
						((VCDSignal<DoubleVal>)s).addSignalChange(maxTime, (DoubleVal) val);
				}
			}
		}
		pcs.firePropertyChange(IWaveformDbLoader.LOADING_FINISHED, null, null);
	}

	public void dispose() {
		moduleStack=null;
		signals=null;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.ITrDb#getMaxTime()
	 */
	@Override
	public Long getMaxTime() {
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
			if("SystemC".compareTo(tokenString)!=0) moduleStack.push(tokenString);
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
	@SuppressWarnings("unchecked")
	@Override
	public Integer newNet(String name, int i, int width) {
		String netName = moduleStack.isEmpty()? name: moduleStack.peek()+"."+name;
		int id = signals.size();
		if(width==0) {
			signals.add( i<0 ? new VCDSignal<DoubleVal>(id, netName, width) :
				new VCDSignal<DoubleVal>((VCDSignal<DoubleVal>)signals.get(i), id, netName));			
		} else if(width>0){
			signals.add( i<0 ? new VCDSignal<BitVector>(id, netName, width) :
				new VCDSignal<BitVector>((VCDSignal<BitVector>)signals.get(i), id, netName));
		}
		pcs.firePropertyChange(IWaveformDbLoader.SIGNAL_ADDED, null, Iterables.getLast(signals));
		return id;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#getNetWidth(int)
	 */
	@Override
	public int getNetWidth(int intValue) {
		VCDSignal<?> signal = (VCDSignal<?>) signals.get(intValue);
		return signal.getRowCount();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#appendTransition(int, long, com.minres.scviewer.database.vcd.BitVector)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void appendTransition(int signalId, long currentTime, BitVector value) {
		VCDSignal<BitVector> signal = (VCDSignal<BitVector>) signals.get(signalId);
		Long time = currentTime* TIME_RES;
		signal.addSignalChange(time, value);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.vcd.ITraceBuilder#appendTransition(int, long, com.minres.scviewer.database.vcd.BitVector)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void appendTransition(int signalId, long currentTime, double value) {
		VCDSignal<DoubleVal> signal = (VCDSignal<DoubleVal>) signals.get(signalId);
		Long time = currentTime* TIME_RES;
		signal.addSignalChange(time, new DoubleVal(value));
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


}
