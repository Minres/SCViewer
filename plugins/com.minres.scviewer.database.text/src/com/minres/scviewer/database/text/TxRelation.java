package com.minres.scviewer.database.text;

import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

class TxRelation implements ITxRelation {
	final Tx source;
	
	final Tx target;
	
	final RelationType relationType;
	
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
