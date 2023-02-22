/*******************************************************************************
 * Copyright (c) 2023 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/** The relation type. */
	final RelationType relationType;

	/**
	 * Instantiates a new scv relation.
	 *
	 * @param relationType the relation type
	 * @param source       the source
	 * @param target       the target
	 */
	public FtrRelation(RelationType relationType, long source, long target) {
		this.source = source;
		this.target = target;
		this.relationType = relationType;
	}

}
