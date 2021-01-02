package com.minres.scviewer.database;

public class DoubleVal implements IEvent {

	public final double value;
	
	public DoubleVal(double value) {
		this.value=value;
	}

	@Override
	public EventKind getKind() {
		return EventKind.SINGLE;
	}

	@Override
	public WaveformType getType() {
		return WaveformType.SIGNAL;
	}

	@Override
	public IEvent duplicate() throws CloneNotSupportedException {
		return (IEvent) clone();
	}

}
