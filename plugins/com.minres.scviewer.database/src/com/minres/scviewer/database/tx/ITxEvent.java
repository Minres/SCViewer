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
	public Long getTime();

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
