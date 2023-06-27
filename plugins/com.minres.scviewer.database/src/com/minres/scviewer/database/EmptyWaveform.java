/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/

package com.minres.scviewer.database;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class EmptyWaveform implements IWaveform {

	private String label = "";

	
	public EmptyWaveform() {
	}

	public EmptyWaveform(String label) {
		this.label = label;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public String getFullName() {
		return label;
	}

	@Override
	public String getName() {
		return label;
	}

	@Override
	public void setName(String name) {
		label=name;
	}

	@Override
	public void setParent(IHierNode parent) {
	}

	@Override
	public IHierNode getParent() {
		return null;
	}

	@Override
	public List<IHierNode> getChildNodes() {
		return new ArrayList<>();
	}

	@Override
	public void addChild(IHierNode child) {
	}

	@Override
	public IDerivedWaveform deriveWaveform() {
		return null;
	}

	@Override
	public int compareTo(IHierNode o) {
		return 1;
	}

	@Override
	public long getId() {
		return 0;
	}

	@Override
	public boolean isSame(IWaveform other) {
		return false;
	}

	@Override
	public IEventList getEvents() {
		return new EventList();
	}

	@Override
	public IEvent[] getEventsAtTime(long time) {
		return new IEvent[0];
	}

	@Override
	public IEvent[] getEventsBeforeTime(long time) {
		return new IEvent[0];
	}

	@Override
	public WaveformType getType() {
		return WaveformType.EMPTY;
	}

	@Override
	public String getKind() {
		return "separator";
	}

	@Override
	public int getRowCount() {
		return 1;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public DirectionType getDirection() {
		return DirectionType.IMPLICIT;
	}

}
