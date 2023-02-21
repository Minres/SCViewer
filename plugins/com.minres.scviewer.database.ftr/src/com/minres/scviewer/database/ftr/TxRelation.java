/*******************************************************************************
 * Copyright (c) 2020 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

/**
 * The Class TxRelation.
 */
class TxRelation implements ITxRelation {

	/** The loader. */
	final FtrDbLoader loader;

	/** The scv relation. */
	final FtrRelation scvRelation;

	/**
	 * Instantiates a new tx relation.
	 *
	 * @param loader      the loader
	 * @param scvRelation the scv relation
	 */
	public TxRelation(FtrDbLoader loader, FtrRelation scvRelation) {
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
