/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

/**
 * The Interface ITxAttribute.
 */
public interface ITxAttribute extends ITxAttributeType {

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public Object getValue();
}
