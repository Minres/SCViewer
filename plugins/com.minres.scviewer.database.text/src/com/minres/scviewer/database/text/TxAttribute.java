/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2015-2026 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

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

}
