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
package com.minres.scviewer.database.text;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

/**
 * The Class TxEvent.
 */
class TxEvent implements ITxEvent {

	/** The loader. */
	final TextDbLoader loader;

	/** The kind. */
	final EventKind kind;

	/** The transaction. */
	final long transaction;

	/** The time. */
	final long time;

	private int concurrencyIdx=-1;
	/**
	 * Instantiates a new tx event.
	 *
	 * @param loader      the loader
	 * @param kind        the kind
	 * @param transaction the transaction
	 * @param time        the time
	 */
	TxEvent(TextDbLoader loader, EventKind kind, Long transaction, Long time) {
		this.loader = loader;
		this.kind = kind;
		this.transaction = transaction;
		this.time = time;
	}

	/**
	 * Duplicate.
	 *
	 * @return the i tx event
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public ITxEvent duplicate() throws CloneNotSupportedException {
		return new TxEvent(loader, kind, transaction, time);
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return kind.toString() + "@" + time + " of tx #" + transaction;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Override
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	@Override
	public EventKind getKind() {
		return kind;
	}

	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	@Override
	public long getTime() {
		return time;
	}

	/**
	 * Gets the transaction.
	 *
	 * @return the transaction
	 */
	@Override
	public ITx getTransaction() {
		return loader.getTransaction(transaction);
	}

	@Override
	public int getRowIndex() {
		return concurrencyIdx;
	}

	public void setConcurrencyIndex(int idx) {
		concurrencyIdx=idx;
	}
}
