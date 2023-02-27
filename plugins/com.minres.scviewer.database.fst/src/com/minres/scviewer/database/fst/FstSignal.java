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

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;

public class FstSignal<T extends IEvent> extends HierNode implements IWaveform {

	private final FstDbLoader loader;
	
	private final int id;

	private final String fullName;

	private final int width;

	private final IEventList values;
	
	public FstSignal(FstDbLoader loader, String name) {
		this(loader, 0, name, 1);
	}

	public FstSignal(FstDbLoader loader, int id, String name) {
		this(loader, id,name,1);
	}

	public FstSignal(FstDbLoader loader, int id, String name, int width) {
		super(name);
		fullName=name;
		this.loader=loader;
		this.id=id;
		this.width=width;
		this.values=new EventList();
	}

	public FstSignal(FstSignal<T> o, int id, String name) {
		super(name);
		fullName=name;
		this.loader=o.loader;
		this.id=id;
		this.width=o.width;
		this.values=o.values;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public IEventList getEvents() {
		if(values.size()==0)
			loader.getEvents(id, width, values);
		return values;
	}

	@Override
	public IEvent[] getEventsAtTime(long time) {
		return getEvents().get(time);
	}

    @Override
    public IEvent[] getEventsBeforeTime(long time) {
    	EventEntry e = getEvents().floorEntry(time);
    	if(e==null)
    		return new IEvent[] {};
    	else
    		return getEvents().floorEntry(time).events;
    }

	@Override
	public boolean isSame(IWaveform other) {
		return( other instanceof FstSignal<?> && this.getId() == other.getId());
	}

	@Override
	public WaveformType getType() {
		return WaveformType.SIGNAL;
	}

	@Override
	public int getRowCount() {
		return 1;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public String getKind() {
		return "signal";
	}

}
