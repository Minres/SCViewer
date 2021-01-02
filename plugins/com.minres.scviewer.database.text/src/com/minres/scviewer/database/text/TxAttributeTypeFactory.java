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

import java.util.HashMap;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITxAttributeType;

public class TxAttributeTypeFactory {
	public static final TxAttributeTypeFactory INSTANCE = new TxAttributeTypeFactory();
	
	HashMap<String, ITxAttributeType> attributes = new HashMap<>();
	
	private TxAttributeTypeFactory() {}
	
	ITxAttributeType getAttrType(String name, DataType dataType, AssociationType type){
		String key = name+":"+dataType.toString();
		ITxAttributeType res;
		if(attributes.containsKey(key)){
			res=attributes.get(key);
		} else {
			res=new TxAttributeType(name, dataType, type);
			attributes.put(key, res);
		}
		return res;
	}
}
