/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020-2026 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class ScvTx.
 */
class ScvTx implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -855200240003328221L;

	/** The id. */
	final long id;

	/** The generator id. */
	final long generatorId;

	/** The stream id. */
	final long streamId;

	/** The begin time. */
	long beginTime;

	/** The end time. */
	long endTime;

	/** The attributes. */
	final List<ITxAttribute> attributes = new ArrayList<>();

	/**
	 * Instantiates a new scv tx.
	 *
	 * @param id          the id
	 * @param streamId    the stream id
	 * @param generatorId the generator id
	 * @param begin       the begin
	 */
	ScvTx(long id, long streamId, long generatorId, long begin) {
		this.id = id;
		this.streamId = streamId;
		this.generatorId = generatorId;
		this.beginTime = begin;
		this.endTime = begin;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	Long getId() {
		return id;
	}
}
