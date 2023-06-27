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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
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
		ITx tx = loader.getTransaction(scvRelation.source);
		if(tx!=null) return tx;
		return new TxFacade(scvRelation.source_fiber, scvRelation.source);
	}

	/**
	 * Gets the target.
	 *
	 * @return the target
	 */
	@Override
	public ITx getTarget() {
		ITx tx = loader.getTransaction(scvRelation.target);
		if(tx!=null) return tx;
		return new TxFacade(scvRelation.target_fiber, scvRelation.target);
	}

	private class TxFacade implements ITx {

		final long fiberId;
		
		final long txId;
		
		ITx tx = null;
		
		public TxFacade(long fiberId, long txId) {
			this.fiberId = fiberId;
			this.txId=txId;
		}

		@Override
		public int compareTo(ITx o) {
			return tx==null?-1:tx.compareTo(o);
		}

		@Override
		public long getId() {
			return txId;
		}

		@Override
		public IWaveform getStream() {
			if(tx==null) {
				TxStream fiber = loader.txStreams.get(fiberId);
				fiber.loadStream();
				tx = loader.getTransaction(txId);
				return loader.txStreams.get(fiberId);
			} else
				return tx.getStream();
		}

		@Override
		public IWaveform getGenerator() {
			if(tx==null) {
				loader.txStreams.get(fiberId).loadStream();
				tx = loader.getTransaction(txId);
			}
			return tx.getGenerator();
		}

		@Override
		public long getBeginTime() {
			return tx==null?-1:tx.getBeginTime();
		}

		@Override
		public long getEndTime() {
			return tx==null?-1:tx.getBeginTime();
		}

		@Override
		public List<ITxAttribute> getAttributes() {
			return tx==null?new ArrayList<>():tx.getAttributes();
		}

		@Override
		public Collection<ITxRelation> getIncomingRelations() {
			return tx==null?new ArrayList<>():tx.getIncomingRelations();
		}

		@Override
		public Collection<ITxRelation> getOutgoingRelations() {
			return tx==null?new ArrayList<>():tx.getOutgoingRelations();
		}

		@Override
		public String toString() {
			return tx==null?("tx#" + getId() + "[not available]"):tx.toString();
		}
	}
}
