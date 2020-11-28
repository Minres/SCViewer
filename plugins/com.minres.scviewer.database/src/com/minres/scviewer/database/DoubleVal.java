package com.minres.scviewer.database;

public class DoubleVal implements IEvent {

	final double value;
	
	public DoubleVal(double value) {
		this.value=value;
	}

	@Override
	public EventKind getKind() {
		return EventKind.SINGLE;
	}

	@Override
	public Class<?> getType() {
		return this.getClass();
	}

	@Override
	public IEvent duplicate() throws CloneNotSupportedException {
		return (IEvent) clone();
	}

}
