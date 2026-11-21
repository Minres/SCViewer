/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.sqlite;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

public class TxRelation implements ITxRelation {

	RelationType relationType;
	Tx source;
	Tx target;

	public TxRelation(RelationType relationType, Tx source, Tx target) {
		this.source = source;
		this.target = target;
		this.relationType = relationType;
	}

	@Override
	public RelationType getRelationType() {
		return relationType;
	}

	@Override
	public ITx getSource() {
		return source;
	}

	@Override
	public ITx getTarget() {
		return target;
	}

}
