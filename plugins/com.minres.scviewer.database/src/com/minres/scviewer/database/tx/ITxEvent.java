/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

import com.minres.scviewer.database.IEvent;

/**
 * The Interface ITxEvent.
 */
public interface ITxEvent extends IEvent {

	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public long getTime();

	/**
	 * Gets the transaction.
	 *
	 * @return the transaction
	 */
	public ITx getTransaction();

	/**
	 * Gets the concurrency index.
	 *
	 * @return the concurrency index
	 */
	public int getRowIndex();

}
