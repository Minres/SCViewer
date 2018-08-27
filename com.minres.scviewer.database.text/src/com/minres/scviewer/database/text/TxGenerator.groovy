/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text

import java.util.ArrayList;
import java.util.List;
import org.apache.jdbm.DB
import com.minres.scviewer.database.ITxAttributeType
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxGenerator;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.IWaveformEvent;

class TxGenerator implements ITxGenerator, Serializable{
	Long id
	Long stream_id
	String name
	TextDbLoader loader;
	Boolean active = false
	ArrayList<ITx> transactions=[]
	
	ArrayList<ITxAttributeType> begin_attrs = []
	int begin_attrs_idx = 0
	ArrayList<ITxAttributeType> end_attrs= []
	int end_attrs_idx = 0
	
	TxGenerator(TextDbLoader loader, Long id, Long stream_id, name){
		this.id=id
		this.stream_id=stream_id
		this.name=name
		this.loader=loader
	}
	
	ITxStream<? extends ITxEvent> getStream(){
		return loader.streamsById[stream_id];
	}
	
	List<ITx> getTransactions(){
		return transactions
	}
	
	Boolean isActive() {return active};
	
}
