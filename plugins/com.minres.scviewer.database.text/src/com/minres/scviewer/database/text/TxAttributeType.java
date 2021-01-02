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
import com.minres.scviewer.database.tx.ITxAttributeType;

class TxAttributeType implements ITxAttributeType, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7159721937208946828L;
	
	private String name;
	
	private DataType dataType;
	
	private AssociationType type;
	
	static ITxAttributeType getAttrType(String name, DataType dataType, AssociationType type){
		return TxAttributeTypeFactory.INSTANCE.getAttrType(name, dataType, type);
	}
	
	TxAttributeType(String name, DataType dataType, AssociationType type){
		this.name=name;
		this.dataType=dataType;
		this.type=type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public DataType getDataType() {
		return dataType;
	}

	@Override
	public AssociationType getType() {
		return type;
	}
}
