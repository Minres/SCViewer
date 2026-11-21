/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

/**
 * The Class DoubleVal.
 */
public class DoubleVal implements IEvent {

	/** The value. */
	public final double value;

	/**
	 * Instantiates a new double val.
	 *
	 * @param value the value
	 */
	public DoubleVal(double value) {
		this.value = value;
	}

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	@Override
	public EventKind getKind() {
		return EventKind.SINGLE;
	}

	/**
	 * Duplicate.
	 *
	 * @return the i event
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public IEvent duplicate() throws CloneNotSupportedException {
		return (IEvent) clone();
	}

}
