/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.io.Serializable;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITxAttribute;

public class TxAttribute  implements ITxAttribute, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4767726016651807152L;

	private final TxAttributeType attributeType;

	private final String value;
	
	TxAttribute(TxAttributeType type, String value){
		this.attributeType=type;
		this.value=value;
	}

	@Override
	public String getName() {
		return attributeType.getName();
	}

	@Override
	public AssociationType getType() {
		return attributeType.getType();
	}

	@Override
	public DataType getDataType() {
		return attributeType.getDataType();
	}

	@Override
	public Object getValue() {
		return value;
	}

}
