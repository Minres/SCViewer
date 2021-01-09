package com.minres.scviewer.database.text;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

class TxEvent implements ITxEvent {

	final TextDbLoader loader;
	
	final EventKind kind;
	
	final Long transaction;
	
	final Long time;
	
	TxEvent(TextDbLoader loader, EventKind kind, Long transaction, Long time) {
		this.loader=loader;
		this.kind = kind;
		this.transaction = transaction;
		this.time = time;
	}

	@Override
	public
	ITxEvent duplicate() throws CloneNotSupportedException {
		return new TxEvent(loader, kind, transaction, time);
	}

	@Override
	public
	String toString() {
		return kind.toString()+"@"+time+" of tx #"+transaction;
	}

	@Override
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}

	@Override
	public EventKind getKind() {
		return kind;
	}

	@Override
	public Long getTime() {
		return time;
	}

	@Override
	public ITx getTransaction() {
		return loader.getTransaction(transaction);
	}
}
