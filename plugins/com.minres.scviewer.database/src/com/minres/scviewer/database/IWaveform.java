/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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

    /** Recorded inputs used to coordinate derived reads with database closure. */
    default java.util.List<IWaveform> getReadSources() { return java.util.List.of(this); }

    /** Signal metadata; real-valued loaders override this without reading events. */
    default boolean isBitVector() { return getType() == WaveformType.SIGNAL && getWidth() > 0; }


	/** User-facing label; derived waveforms may have a separate persistent identity. */
	default String getDisplayName() { return getFullName(); }

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
