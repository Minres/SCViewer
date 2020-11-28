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
import com.minres.scviewer.database.IWaveformDb;

public class VCDSignal<T extends IEvent> extends HierNode implements IWaveform {

	private long id;

	private String fullName;

	private final String kind = "signal";
	
	private final int width;

	private final T dummy = null;
	
	private IWaveformDb db;

	private NavigableMap<Long, IEvent[]> values;
	
	public VCDSignal(IWaveformDb db, String name) {
		this(db, 0, name, 1);
	}

	public VCDSignal(IWaveformDb db, int id, String name) {
		this(db, id,name,1);
	}

	public VCDSignal(IWaveformDb db, int id, String name, int width) {
		super(name);
		this.db=db;
		fullName=name;
		this.id=id;
		this.width=width;
		this.values=new TreeMap<Long, IEvent[]>();
	}

	public VCDSignal(VCDSignal<T> other, int id, String name) {
		super(name);
		fullName=name;
		this.id=id;
		assert(other instanceof VCDSignal<?>);
		VCDSignal<T> o = (VCDSignal<T>)other;
		this.width=o.width;
		this.values=o.values;
		this.db=other.getDb();
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

	@Override
	public String getKind() {
		return kind;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public IWaveformDb getDb() {
		return db;
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
    		return null;
    	else
    		return  values.floorEntry(time).getValue();
    }

	@Override
	public Boolean equals(IWaveform other) {
		return( other instanceof VCDSignal<?> && this.getId().equals(other.getId()));
	}

	@Override
	public Class<?> getType() {
		return dummy.getClass();
	}

	@Override
	public int getMaxConcurrency() {
		return 1;
	}

}
