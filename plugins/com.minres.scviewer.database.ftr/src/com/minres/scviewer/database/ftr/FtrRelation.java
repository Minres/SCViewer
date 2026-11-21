/*******************************************************************************
 * Copyright (c) 2021-2023 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.io.Serializable;

import com.minres.scviewer.database.RelationType;

/**
 * The Class ScvRelation.
 */
class FtrRelation implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -347668857680574140L;

	/** The source. */
	final long source;

	/** The target. */
	final long target;

	/** The source. */
	final long source_fiber;

	/** The target. */
	final long target_fiber;

	/** The relation type. */
	final RelationType relationType;

	/**
	 * Instantiates a new scv relation.
	 *
	 * @param relationType the relation type
	 * @param source       the source
	 * @param target       the target
	 */
	public FtrRelation(RelationType relationType, long source, long target, long source_fiber, long target_fiber) {
		this.source = source;
		this.target = target;
		this.source_fiber = source_fiber;
		this.target_fiber = target_fiber;
		this.relationType = relationType;
	}

}
