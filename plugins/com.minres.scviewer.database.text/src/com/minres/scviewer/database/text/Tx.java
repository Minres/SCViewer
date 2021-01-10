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

	/** The id. */
	private long id;

	/** The begin time. */
	long beginTime = -1;

	/** The end time. */
	long endTime = -1;

	/** The concurrency index. */
	private int concurrencyIndex;

	/**
	 * Instantiates a new tx.
	 *
	 * @param loader the loader
	 * @param scvTx  the scv tx
	 */
	public Tx(TextDbLoader loader, ScvTx scvTx) {
		this.loader = loader;
		id = scvTx.id;
	}

	/**
	 * Instantiates a new tx.
	 *
	 * @param loader the loader
	 * @param txId   the tx id
	 */
	public Tx(TextDbLoader loader, long txId) {
		this.loader = loader;
		id = txId;
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
		return loader.txStreams.get(getScvTx().streamId);
	}

	/**
	 * Gets the generator.
	 *
	 * @return the generator
	 */
	@Override
	public ITxGenerator getGenerator() {
		return loader.txGenerators.get(getScvTx().generatorId);
	}

	/**
	 * Gets the begin time.
	 *
	 * @return the begin time
	 */
	@Override
	public Long getBeginTime() {
		if (beginTime < 0)
			beginTime = getScvTx().beginTime;
		return beginTime;
	}

	/**
	 * Gets the end time.
	 *
	 * @return the end time
	 */
	@Override
	public Long getEndTime() {
		if (endTime < 0)
			endTime = getScvTx().endTime;
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
	 * Gets the concurrency index.
	 *
	 * @return the concurrency index
	 */
	@Override
	public int getConcurrencyIndex() {
		return concurrencyIndex;
	}

	/**
	 * Sets the concurrency index.
	 *
	 * @param idx the new concurrency index
	 */
	void setConcurrencyIndex(int idx) {
		concurrencyIndex = idx;
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

	/**
	 * Gets the scv tx.
	 *
	 * @return the scv tx
	 */
	private ScvTx getScvTx() {
		return loader.transactions.get(id);
	}
}
