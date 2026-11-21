/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.LazyValue;
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
		
		private final LazyValue<ITx> resolved = new LazyValue<>();

        private ITx resolve() {
            return resolved.get(() -> {
                ITx transaction = loader.getTransaction(txId);
                if (transaction != null) return transaction;
                TxStream stream = loader.txStreams.get(fiberId);
                if (stream != null) {
                    stream.loadStream();
                    transaction = loader.getTransaction(txId);
                } else if (fiberId < 0) {
                    // Older FTR relations do not record the endpoint stream.
                    for (TxStream candidate : loader.txStreams.values()) {
                        candidate.loadStream();
                        transaction = loader.getTransaction(txId);
                        if (transaction != null) break;
                    }
                }
                if (transaction == null)
                    throw new IllegalStateException("Missing related transaction " + txId + " in stream " + fiberId);
                return transaction;
            });
        }

		public TxFacade(long fiberId, long txId) {
			this.fiberId = fiberId;
			this.txId=txId;
		}

		@Override
		public int compareTo(ITx o) {
			if (this == o) return 0;
            int cmp = Long.compare(getBeginTime(), o.getBeginTime());
            return cmp != 0 ? cmp : Long.compare(getId(), o.getId());
		}

		@Override
		public long getId() {
			return txId;
		}

		@Override
		public IWaveform getStream() {
            return resolve().getStream();
        }

        @Override
        public IWaveform getGenerator() {
            return resolve().getGenerator();
        }

        @Override
        public int getConcurrencyIndxInStream() {
            return resolve().getConcurrencyIndxInStream();
        }

        @Override
        public int getConcurrencyIdxInGen() {
            return resolve().getConcurrencyIdxInGen();
        }

		@Override
		public long getBeginTime() {
			return resolve().getBeginTime();
		}

		@Override
		public long getEndTime() {
			return resolve().getEndTime();
		}

		@Override
		public List<ITxAttribute> getAttributes() {
			return resolve().getAttributes();
		}

		@Override
		public Collection<ITxRelation> getIncomingRelations() {
			return resolve().getIncomingRelations();
		}

		@Override
		public Collection<ITxRelation> getOutgoingRelations() {
			return resolve().getOutgoingRelations();
		}

		@Override
		public String toString() {
			return "tx#" + txId + "[related transaction]";
		}
	}
}
