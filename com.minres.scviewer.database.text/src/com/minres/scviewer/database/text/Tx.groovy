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
package com.minres.scviewer.database.text

import com.minres.scviewer.database.*

class Tx implements ITx, Serializable {
	
	TextDbLoader loader
	
	Long id
	
	Long generator_id
	
	int concurrencyIndex
	
	Long beginTime
	
	Long endTime
	
	ArrayList<ITxAttribute> attributes = new ArrayList<ITxAttribute>()
	
	def incomingRelations =[]
	
	def outgoingRelations =[]
	
	Tx(TextDbLoader loader, Long id, Long generator_id, Long begin){
		this.loader=loader
		this.id=id
		this.generator_id=generator_id
		this.beginTime=begin
        this.endTime=begin
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
		def res =beginTime.compareTo(o.beginTime)
		if(res!=0) 
			return res
		else
			return id.compareTo(o.id)
	}
	
	@Override
	public String toString() {
		return "tx#"+getId()+"["+getBeginTime()/1000000+"ns - "+getEndTime()/1000000+"ns]";
	}

	@Override
	public ITxStream<ITxEvent> getStream() {
		return generator.stream;
	}

	@Override
	public ITxGenerator getGenerator() {
		return loader.generatorsById[generator_id];
	}

}
