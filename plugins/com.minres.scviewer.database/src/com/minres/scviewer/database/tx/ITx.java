/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.IWaveform;

/**
 * The Interface ITx.
 */
public interface ITx extends Comparable<ITx> {

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public long getId();

	/**
	 * Gets the stream.
	 *
	 * @return the stream
	 */
	public IWaveform getStream();

	/**
	 * Gets the generator.
	 *
	 * @return the generator
	 */
	public IWaveform getGenerator();

    /**
     * Gets the zero-based row index within the transaction's stream.
     * The stream layout is calculated on demand.
     *
     * @return the concurrency index in the stream
     */
    public int getConcurrencyIndxInStream();

    /**
     * Gets the zero-based row index within the transaction's generator.
     * The generator layout is calculated on demand, independently of the stream layout.
     *
     * @return the concurrency index in the generator
     */
    public int getConcurrencyIdxInGen();

	/**
	 * Gets the begin time.
	 *
	 * @return the begin time
	 */
	public long getBeginTime();

	/**
	 * Gets the end time.
	 *
	 * @return the end time
	 */
	public long getEndTime();

	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	public List<ITxAttribute> getAttributes();

	/**
	 * Gets the incoming relations.
	 *
	 * @return the incoming relations
	 */
	public Collection<ITxRelation> getIncomingRelations();

	/**
	 * Gets the outgoing relations.
	 *
	 * @return the outgoing relations
	 */
	public Collection<ITxRelation> getOutgoingRelations();
}
