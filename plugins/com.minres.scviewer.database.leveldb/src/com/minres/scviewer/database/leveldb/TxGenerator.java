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
package com.minres.scviewer.database.leveldb;

import java.util.List;

import org.json.JSONObject;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxGenerator;
import com.minres.scviewer.database.ITxStream;

public class TxGenerator implements ITxGenerator {

	private ITxStream<ITxEvent>  stream;
	
	private long id;
	
	private String name;

	public TxGenerator(ITxStream<ITxEvent>  stream, JSONObject object) {
		this.stream=stream;
		this.id=object.getLong("id");
		this.name=object.getString("name");
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public ITxStream<ITxEvent> getStream() {
		return stream;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<ITx> getTransactions() {
		return null;
	}

}
