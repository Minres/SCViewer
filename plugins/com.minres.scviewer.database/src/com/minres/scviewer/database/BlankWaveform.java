package com.minres.scviewer.database;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class BlankWaveform implements IWaveform {

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public String getFullName() {
		return "";
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public void setName(String name) {
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
		return WaveformType.BLANK;
	}

	@Override
	public String getKind() {
		return "BLANK";
	}

	@Override
	public int getRowCount() {
		return 1;
	}

	@Override
	public int getWidth() {
		return 0;
	}

}
