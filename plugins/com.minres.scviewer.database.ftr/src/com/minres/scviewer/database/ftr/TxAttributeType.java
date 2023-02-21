/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.io.Serializable;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITxAttributeType;

/**
 * The Class TxAttributeType.
 */
class TxAttributeType implements ITxAttributeType, Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7159721937208946828L;

	/** The name. */
	private String name;

	/** The data type. */
	private DataType dataType;

	/** The type. */
	private AssociationType type;

	/**
	 * Instantiates a new tx attribute type.
	 *
	 * @param name     the name
	 * @param dataType the data type
	 * @param type     the type
	 */
	TxAttributeType(String name, DataType dataType, AssociationType type) {
		this.name = name;
		this.dataType = dataType;
		this.type = type;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Gets the data type.
	 *
	 * @return the data type
	 */
	@Override
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Override
	public AssociationType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return name + ":" + dataType.name() + "@" + type.name();
	}
}
