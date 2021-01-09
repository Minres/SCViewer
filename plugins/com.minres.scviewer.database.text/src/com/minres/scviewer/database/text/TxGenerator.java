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

import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITxGenerator;

class TxGenerator extends AbstractTxStream implements ITxGenerator {

	TxStream stream;
		
	List<TxAttributeType> beginAttrs = new ArrayList<>();
	
	List<TxAttributeType> endAttrs= new ArrayList<>();

	TxGenerator(TextDbLoader loader, Long id, String name, TxStream stream){
		super(loader, id, name);
		this.stream=stream;
	}
	
	@Override
	public IWaveform getStream(){
		return stream;
	}
	
	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxGenerator && this.getId().equals(other.getId()));
	}
	
	public List<TxAttributeType> getBeginAttrs() {
		return beginAttrs;
	}

	public List<TxAttributeType> getEndAttrs() {
		return endAttrs;
	}

	@Override
	public String getKind() {
		return stream.getKind();
	}
}
