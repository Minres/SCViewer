/*******************************************************************************
 * Copyright (c) 2023 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.DirectionType;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.InputFormatException;

/**
 * The Class TxGenerator.
 */
class TxGenerator extends AbstractTxStream {

	/** The stream. */
	TxStream stream;

	/** The begin attrs. */
	List<TxAttributeType> beginAttrs = new ArrayList<>();

	/** The end attrs. */
	List<TxAttributeType> endAttrs = new ArrayList<>();

	/**
	 * Instantiates a new tx generator.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 * @param stream the stream
	 */
	TxGenerator(FtrDbLoader loader, Long id, String name, TxStream stream) {
		super(loader, id, name);
		this.stream = stream;
		stream.addChild(this);
	}

	/**
	 * Checks if is same.
	 *
	 * @param other the other
	 * @return true, if is same
	 */
	@Override
	public boolean isSame(IWaveform other) {
		return (other instanceof TxGenerator && this.getId()==other.getId());
	}

	/**
	 * Gets the events.
	 *
	 * @return the events
	 */
	@Override
	public IEventList getEvents() {
		if(events.size()==0) {
			try {
				List<byte[]> chunks = stream.getChunks();
				int blockid = 0;
				for (byte[] bs : chunks) {
					loader.parseTx(stream, blockid, bs);
				}
			} catch (InputFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return events;
	}
	/**
	 * Gets the begin attrs.
	 *
	 * @return the begin attrs
	 */
	public List<TxAttributeType> getBeginAttrs() {
		return beginAttrs;
	}

	/**
	 * Gets the end attrs.
	 *
	 * @return the end attrs
	 */
	public List<TxAttributeType> getEndAttrs() {
		return endAttrs;
	}

	/**
	 * Gets the kind.
	 *
	 * @return the kind
	 */
	@Override
	public String getKind() {
		return stream.getKind();
	}
	
	/**
	 * Gets the full hierarchical name.
	 *
	 * @return the full name
	 */
	@Override
	public String getFullName() {
		return  ((AbstractTxStream)parent).getFullName()+"."+name;
	}

	@Override
	public DirectionType getDirection() {
		return DirectionType.IMPLICIT;
	}
}
