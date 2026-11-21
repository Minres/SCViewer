/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

/**
 * The Class TxRelation.
 */
class TxRelation implements ITxRelation {

	/** The loader. */
	final TextDbLoader loader;

	/** The scv relation. */
	final ScvRelation scvRelation;

	/**
	 * Instantiates a new tx relation.
	 *
	 * @param loader      the loader
	 * @param scvRelation the scv relation
	 */
	public TxRelation(TextDbLoader loader, ScvRelation scvRelation) {
		this.loader = loader;
		this.scvRelation = scvRelation;
	}

	/**
	 * Gets the relation type.
	 *
	 * @return the relation type
	 */
	@Override
	public RelationType getRelationType() {
		return scvRelation.relationType;
	}

	/**
	 * Gets the source.
	 *
	 * @return the source
	 */
	@Override
	public ITx getSource() {
		return loader.getTransaction(scvRelation.source);
	}

	/**
	 * Gets the target.
	 *
	 * @return the target
	 */
	@Override
	public ITx getTarget() {
		return loader.getTransaction(scvRelation.target);
	}

}
