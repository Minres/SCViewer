/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.io.Serializable;

import com.minres.scviewer.database.RelationType;

/**
 * The Class ScvRelation.
 */
class ScvRelation implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -347668857680574140L;

	/** The source. */
	final long source;

	/** The target. */
	final long target;

	/** The relation type. */
	final RelationType relationType;

	/**
	 * Instantiates a new scv relation.
	 *
	 * @param relationType the relation type
	 * @param source       the source
	 * @param target       the target
	 */
	public ScvRelation(RelationType relationType, long source, long target) {
		this.source = source;
		this.target = target;
		this.relationType = relationType;
	}

}
