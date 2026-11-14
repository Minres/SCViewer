/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.sqlite;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

public class TxEvent implements ITxEvent {

	private final EventKind type;
    private final boolean inGenerator;
	private ITx tx;
	
	public TxEvent(EventKind type, ITx tx) {
        this(type, tx, false);
    }

    TxEvent(EventKind type, ITx tx, boolean inGenerator) {
        this.type = type;
        this.tx = tx;
        this.inGenerator = inGenerator;
	}

	@Override
	public long getTime() {
		return type==EventKind.BEGIN?tx.getBeginTime():tx.getEndTime();
	}

	@Override
	public IEvent duplicate() throws CloneNotSupportedException {
		return new TxEvent(type, tx, inGenerator);
	}

	@Override
	public ITx getTransaction() {
		return tx;
	}

	@Override
	public EventKind getKind() {
		return type;
	}

	@Override
	public String toString() {
		return type.toString()+"@"+getTime()+" of tx #"+tx.getId();
	}

	@Override
	public int getRowIndex() {
		return inGenerator ? tx.getConcurrencyIdxInGen() : tx.getConcurrencyIndxInStream();
	}

}
