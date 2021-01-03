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
import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.tx.ITxAttribute;

class ScvTx implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -855200240003328221L;

	final long id;
	
	final long generatorId;

	final long streamId;
	
	long beginTime;
	
	long endTime;
	
	final List<ITxAttribute> attributes = new ArrayList<>();
	
	ScvTx(long id, long streamId, long generatorId, long begin){
		this.id=id;
		this.streamId=streamId;
		this.generatorId=generatorId;
		this.beginTime=begin;
        this.endTime=begin;
	}
	
	Long getId() {return id;}
}
