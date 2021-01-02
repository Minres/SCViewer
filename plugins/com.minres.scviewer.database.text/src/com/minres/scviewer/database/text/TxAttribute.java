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
import com.minres.scviewer.database.tx.ITxAttributeType;

public class TxAttribute  implements ITxAttribute, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4767726016651807152L;

	ITxAttributeType attributeType;

	String value;
	
	TxAttribute(String name, DataType dataType, AssociationType type, String value){
		attributeType = TxAttributeTypeFactory.INSTANCE.getAttrType(name, dataType, type);
		switch(dataType){
			case STRING:
			case ENUMERATION:
				this.value=value.substring(1, value.length()-2);
				break;
			default:
				this.value=value;
		}
	}

	TxAttribute(ITxAttributeType type){
		attributeType=type;
	}
	
	TxAttribute(ITxAttributeType type, String value){
		this(type.getName(), type.getDataType(), type.getType(), value);
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
