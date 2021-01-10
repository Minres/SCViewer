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
 * The Interface IEvent.
 */
public interface IEvent {

	/**
	 * Duplicate.
	 *
	 * @return the i event
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	public IEvent duplicate() throws CloneNotSupportedException;

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	public EventKind getKind();

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public WaveformType getType();

}
