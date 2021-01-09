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
package com.minres.scviewer.database.text;

import com.minres.scviewer.database.IWaveform;

class TxStream extends AbstractTxStream {
	
	final String kind;
	
	TxStream(TextDbLoader loader, Long id, String name, String kind){
		super(loader, id, name);
		this.kind=kind;
	}
	
	@Override
	public boolean isSame(IWaveform other) {
		return(other instanceof TxStream && this.getId().equals(other.getId()));
	}

	@Override
	public String getKind() {
		return kind;
	}

}
