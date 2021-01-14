/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020 MINRES Technologies GmbH
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
import java.util.Set;
import java.util.stream.Collectors;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxGenerator;
import com.minres.scviewer.database.tx.ITxRelation;

/**
 * The Class Tx.
 */
class Tx implements ITx {

	/** The loader. */
	private final TextDbLoader loader;
	
	private ScvTx scvTx =null;

	/** The id. */
	private final long id;

	private final long generatorId;

	private final long streamId;

	/** The begin time. */
	long beginTime = -1;

	/** The end time. */
	long endTime = -1;

	/**
	 * Instantiates a new tx.
	 *
	 * @param loader the loader
	 * @param scvTx  the scv tx
	 */
	public Tx(TextDbLoader loader, ScvTx scvTx) {
		this.loader = loader;
		id = scvTx.id;
		generatorId=scvTx.generatorId;
		streamId=scvTx.streamId;
		beginTime=scvTx.beginTime;
		endTime=scvTx.endTime;
	}

	/**
	 * Instantiates a new tx.
	 *
	 * @param loader the loader
	 * @param txId   the tx id
	 */
	public Tx(TextDbLoader loader, long id, long generatorId,  long streamId) {
		this.loader = loader;
		this.id = id;
		this.generatorId=generatorId;
		this.streamId = streamId;
	}

	/**
	 * Gets the incoming relations.
	 *
	 * @return the incoming relations
	 */
	@Override
	public Collection<ITxRelation> getIncomingRelations() {
		Set<ScvRelation> rels = loader.relationsIn.get(id);
		return rels.stream().map(rel -> new TxRelation(loader, rel)).collect(Collectors.toList());
	}

	/**
	 * Gets the outgoing relations.
	 *
	 * @return the outgoing relations
	 */
	@Override
	public Collection<ITxRelation> getOutgoingRelations() {
		Set<ScvRelation> rels = loader.relationsOut.get(id);
		return rels.stream().map(rel -> new TxRelation(loader, rel)).collect(Collectors.toList());
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(ITx o) {
		int res = getBeginTime().compareTo(o.getBeginTime());
		if (res != 0)
			return res;
		else
			return getId().compareTo(o.getId());
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return this.getScvTx().equals(((Tx) obj).getScvTx());
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return getScvTx().hashCode();
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "tx#" + getId() + "[" + getBeginTime() / 1000000 + "ns - " + getEndTime() / 1000000 + "ns]";
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return getScvTx().id;
	}

	/**
	 * Gets the stream.
	 *
	 * @return the stream
	 */
	@Override
	public IWaveform getStream() {
		return loader.txStreams.get(streamId);
	}

	/**
	 * Gets the generator.
	 *
	 * @return the generator
	 */
	@Override
	public ITxGenerator getGenerator() {
		return loader.txGenerators.get(generatorId);
	}

	/**
	 * Gets the begin time.
	 *
	 * @return the begin time
	 */
	@Override
	public Long getBeginTime() {
		if (beginTime < 0) {
			ScvTx tx = scvTx==null?loader.getScvTx(id):getScvTx();
			beginTime = tx.beginTime;
			endTime = tx.endTime;
		}
		return beginTime;
	}

	/**
	 * Gets the end time.
	 *
	 * @return the end time
	 */
	@Override
	public Long getEndTime() {
		if (endTime < 0) {
			ScvTx tx = scvTx==null?loader.getScvTx(id):getScvTx();
			beginTime = tx.beginTime;
			endTime = tx.endTime;
		}
		return endTime;
	}

	/**
	 * Sets the end time.
	 *
	 * @param time the new end time
	 */
	void setEndTime(Long time) {
		getScvTx().endTime = time;
	}

	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	@Override
	public List<ITxAttribute> getAttributes() {
		return getScvTx().attributes;
	}

	private ScvTx getScvTx() {
		if(scvTx==null)
			scvTx=loader.getScvTx(id);
		return scvTx;
	}
}
