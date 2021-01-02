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
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.*;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxGenerator;
import com.minres.scviewer.database.tx.ITxRelation;

class Tx implements ITx, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -855200240003328221L;

	private final Long id;
	
	private final TxGenerator generator;

	private final IWaveform stream;
	
	private int concurrencyIndex;
	
	private final Long beginTime;
	
	private Long endTime;
	
	private final List<ITxAttribute> attributes = new ArrayList<>();
	
	private final List<ITxRelation> incomingRelations = new ArrayList<>();
	
	private final List<ITxRelation> outgoingRelations = new ArrayList<>();
	
	Tx(Long id, IWaveform stream, TxGenerator generator, Long begin){
		this.id=id;
		this.stream=stream;
		this.generator=generator;
		this.beginTime=begin;
        this.endTime=begin;
	}
	
	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		return incomingRelations;
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		return outgoingRelations;
	}

	@Override
	public int compareTo(ITx o) {
		int res =beginTime.compareTo(o.getBeginTime());
		if(res!=0) 
			return res;
		else
			return id.compareTo(o.getId());
	}
	
	@Override
	public String toString() {
		return "tx#"+getId()+"["+getBeginTime()/1000000+"ns - "+getEndTime()/1000000+"ns]";
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public IWaveform getStream() {
		return stream;
	}

	@Override
	public ITxGenerator getGenerator() {
		return generator;
	}

	@Override
	public Long getBeginTime() {
		return beginTime;
	}

	@Override
	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(long l) {
		endTime=l;
	}

	@Override
	public int getConcurrencyIndex() {
		return concurrencyIndex;
	}

	public void setConcurrencyIndex(int idx) {
		concurrencyIndex=idx;
	}

	@Override
	public List<ITxAttribute> getAttributes() {
		return attributes;
	}

}
