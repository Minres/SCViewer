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

import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.sqlite.tables.ScvGenerator;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxGenerator;

public class TxGenerator implements ITxGenerator {

	private IWaveform  stream;
	
	private ScvGenerator scvGenerator;

	public TxGenerator(IWaveform stream, ScvGenerator scvGenerator) {
		this.stream=stream;
		this.scvGenerator=scvGenerator;
	}

	@Override
	public Long getId() {
		return (long) scvGenerator.getId();
	}

	@Override
	public IWaveform getStream() {
		return stream;
	}

	@Override
	public String getName() {
		return scvGenerator.getName();
	}

	@Override
	public List<ITx> getTransactions() {
		return new ArrayList<>();
	}

}
