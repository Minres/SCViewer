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
package com.minres.scviewer.database.vcd;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;

public class VCDSignal<T extends IEvent> extends HierNode implements IWaveform {

	private long id;

	private String fullName;

	private final int width;

	private NavigableMap<Long, IEvent[]> values;
	
	public VCDSignal(String name) {
		this(0, name, 1);
	}

	public VCDSignal(int id, String name) {
		this(id,name,1);
	}

	public VCDSignal(int id, String name, int width) {
		super(name);
		fullName=name;
		this.id=id;
		this.width=width;
		this.values=new TreeMap<>();
	}

	public VCDSignal(VCDSignal<T> o, int id, String name) {
		super(name);
		fullName=name;
		this.id=id;
		this.width=o.width;
		this.values=o.values;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	public void setId(int id) {
		this.id=id;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void addSignalChange(Long time, T value){
		if(values.containsKey(time)) {
			IEvent[] oldV = values.get(time);
			IEvent[] newV = new IEvent[oldV.length+1];
			System.arraycopy(oldV, 0, newV, 0, oldV.length);
			newV[oldV.length]=value;
			values.put(time, newV);
		} else {
			values.put(time, new IEvent[] {value});
		}
	}
	
	@Override
	public NavigableMap<Long, IEvent[]> getEvents() {
		return values;
	}

	@Override
	public IEvent[] getEventsAtTime(Long time) {
		return values.get(time);
	}

    @Override
    public IEvent[] getEventsBeforeTime(Long time) {
    	Entry<Long, IEvent[]> e = values.floorEntry(time);
    	if(e==null)
    		return new IEvent[] {};
    	else
    		return values.floorEntry(time).getValue();
    }

	@Override
	public boolean isSame(IWaveform other) {
		return( other instanceof VCDSignal<?> && this.getId().equals(other.getId()));
	}

	@Override
	public WaveformType getType() {
		return WaveformType.SIGNAL;
	}

	@Override
	public int getWidth() {
		return width;
	}

}
