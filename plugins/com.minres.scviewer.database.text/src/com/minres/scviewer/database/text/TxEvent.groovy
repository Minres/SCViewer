package com.minres.scviewer.database.text;

import com.minres.scviewer.database.EventKind
import com.minres.scviewer.database.ITx
import com.minres.scviewer.database.ITxEvent

class TxEvent implements ITxEvent {

	final EventKind kind;
	
	final Tx transaction;
	
	final Long time
	
	TxEvent(EventKind kind, ITx transaction) {
		super();
		this.kind = kind;
		this.transaction = transaction;
		this.time = kind==EventKind.BEGIN?transaction.beginTime:transaction.endTime
	}

	@Override
	ITxEvent duplicate() throws CloneNotSupportedException {
		new TxEvent(type, transaction, time)
	}

//	@Override
//	int compareTo(IWaveformEvent o) {
//		time.compareTo(o.time)
//	}

	@Override
	String toString() {
		kind.toString()+"@"+time+" of tx #"+transaction.id;
	}

	@Override
	Class<?> getType() {
		return this.getClass();
	}
}
