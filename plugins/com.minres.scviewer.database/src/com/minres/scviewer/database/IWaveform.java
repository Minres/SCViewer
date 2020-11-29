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
package com.minres.scviewer.database;

import java.util.NavigableMap;

public interface IWaveform extends IHierNode {

	public Long getId();

	public IWaveformDb getDb();
	
	public boolean isSame(IWaveform other);

	public NavigableMap<Long, IEvent[]> getEvents();

	public IEvent[] getEventsAtTime(Long time);

	public IEvent[] getEventsBeforeTime(Long time);
	
	public WaveformType getType();
	
	public int getWidth();

}
