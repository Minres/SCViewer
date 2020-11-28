/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
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
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.WaveformType;

public class TxEvent implements ITxEvent {

	private final EventKind type;
	private ITx tx;
	
	public TxEvent(EventKind type, ITx tx) {
		super();
		this.type = type;
		this.tx = tx;
	}

	@Override
	public Long getTime() {
		return type==EventKind.BEGIN?tx.getBeginTime():tx.getEndTime();
	}

	@Override
	public IEvent duplicate() throws CloneNotSupportedException {
		return new TxEvent(type, tx);
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
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}
}
