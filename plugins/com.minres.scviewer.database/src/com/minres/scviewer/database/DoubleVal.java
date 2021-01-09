/*******************************************************************************
 * Copyright (c) 2020 MINRES Technologies GmbH and others.
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
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Override
	public WaveformType getType() {
		return WaveformType.SIGNAL;
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
