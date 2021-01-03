package com.minres.scviewer.database.text;

import java.io.Serializable;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

class TxRelation implements ITxRelation, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -347668857680574140L;

	final TextDbLoader loader;
	
	final ScvRelation scvRelation;
	
	public TxRelation(TextDbLoader loader, ScvRelation scvRelation) {
		this.loader = loader;
		this.scvRelation = scvRelation;
	}

	@Override
	public RelationType getRelationType() {
		return scvRelation.relationType;
	}

	@Override
	public ITx getSource() {
		return loader.getTransaction(scvRelation.source);
	}

	@Override
	public ITx getTarget() {
		return loader.getTransaction(scvRelation.target);
	}

}
