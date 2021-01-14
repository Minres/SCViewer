/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020 MINRES Technologies GmbH
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

/**
 * The Class TxStream.
 */
class TxStream extends AbstractTxStream {

	/** The kind. */
	final String kind;

	/**
	 * Instantiates a new tx stream.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 * @param kind   the kind
	 */
	TxStream(TextDbLoader loader, Long id, String name, String kind) {
		super(loader, id, name);
		this.kind = kind;
	}

	/**
	 * Checks if is same.
	 *
	 * @param other the other
	 * @return true, if is same
	 */
	@Override
	public boolean isSame(IWaveform other) {
		return (other instanceof TxStream && this.getId().equals(other.getId()));
	}

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	@Override
	public String getKind() {
		return kind;
	}

}
