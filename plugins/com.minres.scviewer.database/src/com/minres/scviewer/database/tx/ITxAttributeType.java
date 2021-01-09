/*******************************************************************************
 * Copyright (c) 2015 - 2020 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
