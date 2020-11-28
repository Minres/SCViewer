package com.minres.scviewer.database;

public interface IEvent {

	public IEvent duplicate() throws CloneNotSupportedException;

	public EventKind getKind();
	
	public Class<?> getType();

}
