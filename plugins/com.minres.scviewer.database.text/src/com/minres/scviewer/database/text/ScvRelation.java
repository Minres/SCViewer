package com.minres.scviewer.database.text;

import java.io.Serializable;

import com.minres.scviewer.database.RelationType;

class ScvRelation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -347668857680574140L;

	final long source;
	
	final long target;
	
	final RelationType relationType;
	
	public ScvRelation(RelationType relationType, long source, long target) {
		this.source = source;
		this.target = target;
		this.relationType = relationType;
	}

}
