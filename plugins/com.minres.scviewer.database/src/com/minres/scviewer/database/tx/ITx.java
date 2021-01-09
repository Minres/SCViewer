/*******************************************************************************
 * Copyright (c) 2015 - 2020 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public Long getId();

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
	public ITxGenerator getGenerator();

	/**
	 * Gets the begin time.
	 *
	 * @return the begin time
	 */
	public Long getBeginTime();

	/**
	 * Gets the end time.
	 *
	 * @return the end time
	 */
	public Long getEndTime();

	/**
	 * Gets the concurrency index.
	 *
	 * @return the concurrency index
	 */
	public int getConcurrencyIndex();

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
