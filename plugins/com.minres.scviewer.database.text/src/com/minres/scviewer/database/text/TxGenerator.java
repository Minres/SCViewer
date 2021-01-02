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

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttributeType;
import com.minres.scviewer.database.tx.ITxGenerator;

class TxGenerator implements ITxGenerator, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1477511353554904763L;

	Long id;

	IWaveform stream;
	
	String name;
	
	Boolean active = false;
	
	ArrayList<ITx> transactions=new ArrayList<ITx>();
	
	ArrayList<ITxAttributeType> beginAttrs = new ArrayList<ITxAttributeType>();
	
	ArrayList<ITxAttributeType> endAttrs= new ArrayList<ITxAttributeType>();

	TxGenerator(Long id, TxStream stream, String name){
		this.id=id;
		this.stream=stream;
		this.name=name;
	}
	
	public IWaveform getStream(){
		return stream;
	}
	
	public List<ITx> getTransactions(){
		return transactions;
	}
	
	Boolean isActive() {return active;}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public ArrayList<ITxAttributeType> getBeginAttrs() {
		return beginAttrs;
	}

	public ArrayList<ITxAttributeType> getEndAttrs() {
		return endAttrs;
	}
	
}
