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
package com.minres.scviewer.database;



/**
 * The Interface IWaveform.
 *
 * @author eyck
 */
public interface IWaveform extends IHierNode {

	public DirectionType getDirection();
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public long getId();

	/**
	 * Checks if is same.
	 *
	 * @param other the other
	 * @return true, if is same
	 */
	public boolean isSame(IWaveform other);

	/**
	 * Gets the events.
	 *
	 * @return the events
	 */
	public IEventList getEvents();

	/**
	 * Gets the events at time.
	 *
	 * @param time the time
	 * @return the events at time
	 */
	public IEvent[] getEventsAtTime(long time);

	/**
	 * Gets the events before time.
	 *
	 * @param time the time
	 * @return the events before time
	 */
	public IEvent[] getEventsBeforeTime(long time);

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public WaveformType getType();

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	public String getKind();

	/**
	 * Gets the width.
	 *
	 * @return the width
	 */
	public int getRowCount();
	
	/**
	 * Gets the width.
	 *
	 * @return the width
	 */
	public int getWidth();
	
}
