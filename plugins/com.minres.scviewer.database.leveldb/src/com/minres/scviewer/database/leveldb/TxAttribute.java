/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.leveldb;

import org.json.JSONObject;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITxAttribute;

public class TxAttribute implements ITxAttribute{

	private String name;
	
	private DataType dataType;
	
	private AssociationType associationType;

	private Object value;
	
	public TxAttribute(Tx trTransaction, JSONObject attribute) {
		this.name=attribute.getString("name");
		this.dataType=DataType.values()[attribute.getInt("type")];
		this.associationType=AssociationType.values()[attribute.getInt("assoc")];
		this.value=attribute.get("value");
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
		return associationType;
	}

	@Override
	public Object getValue() {
		return value;
	}

}
