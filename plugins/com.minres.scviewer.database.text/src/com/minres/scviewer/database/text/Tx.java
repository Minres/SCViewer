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

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxGenerator;
import com.minres.scviewer.database.tx.ITxRelation;

class Tx implements ITx {
	
	private final TextDbLoader loader;
	
	private ScvTx scvTx = null;
	
	private int concurrencyIndex;
				
	public Tx(TextDbLoader loader, ScvTx scvTx) {
		this.loader=loader;
		this.scvTx = scvTx;
	}

	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		Set<ScvRelation> rels = loader.relationsIn.get(scvTx.getId());
		return rels.stream().map(rel -> new TxRelation(loader, rel)).collect(Collectors.toList());
	}

	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		Set<ScvRelation> rels = loader.relationsOut.get(scvTx.getId());
		return rels.stream().map(rel -> new TxRelation(loader, rel)).collect(Collectors.toList());
	}

	@Override
	public int compareTo(ITx o) {
		int res =getBeginTime().compareTo(o.getBeginTime());
		if(res!=0) 
			return res;
		else
			return getId().compareTo(o.getId());
	}
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return this.scvTx.equals(((Tx) obj).scvTx);
    }

	@Override
    public int hashCode() {
		return scvTx.hashCode();
	}

	@Override
	public String toString() {
		return "tx#"+getId()+"["+getBeginTime()/1000000+"ns - "+getEndTime()/1000000+"ns]";
	}

	@Override
	public Long getId() {
		return scvTx.id;
	}

	@Override
	public IWaveform getStream() {
		return loader.txStreams.get(scvTx.streamId);
	}

	@Override
	public ITxGenerator getGenerator() {
		return loader.txGenerators.get(scvTx.generatorId);
	}

	@Override
	public Long getBeginTime() {
		return scvTx.beginTime;
	}

	@Override
	public Long getEndTime() {
		return scvTx.endTime;
	}

	void setEndTime(Long time) {
		 scvTx.endTime=time;
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
		return scvTx.attributes;
	}

}
