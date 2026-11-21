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

}
