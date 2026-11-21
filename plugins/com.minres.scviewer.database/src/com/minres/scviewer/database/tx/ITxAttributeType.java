/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;

/**
 * The Interface ITxAttributeType.
 */
public interface ITxAttributeType {

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Gets the data type.
	 *
	 * @return the data type
	 */
	public DataType getDataType();

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public AssociationType getType();
}
