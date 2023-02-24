/*******************************************************************************
 * Copyright (c) 2023 MINRES Technologies GmbH
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
import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class TxAttribute.
 */
public class TxAttribute implements ITxAttribute, Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4767726016651807152L;

	/** The attribute type. */
	private final TxAttributeType attributeType;

	/** The value. */
	private final String value;

	/**
	 * Instantiates a new tx attribute.
	 *
	 * @param type  the type
	 * @param value the value
	 */
	TxAttribute(TxAttributeType type, String value) {
		this.attributeType = type;
		this.value = value;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return attributeType.getName();
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Override
	public AssociationType getType() {
		return attributeType.getType();
	}

	/**
	 * Gets the data type.
	 *
	 * @return the data type
	 */
	@Override
	public DataType getDataType() {
		return attributeType.getDataType();
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "FtxAttr: " + attributeType.toString() + "=" + value;
	}
}
