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
import com.minres.scviewer.database.LazyValue;
import com.minres.scviewer.database.InputFormatException;

/**
 * The Class TxStream.
 */
class TxStream extends AbstractTxStream {

	/** The kind. */
	final String kind;

	final ArrayList<Long> fileOffsets = new ArrayList<>();
	
	private final LazyValue<List<byte[]>> chunks = new LazyValue<>();
	private final LazyValue<Boolean> parsed = new LazyValue<>();

	/**
	 * Instantiates a new tx stream.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 * @param kind   the kind
	 */
	TxStream(FtrDbLoader loader, Long id, String name, String kind) {
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
		return (other instanceof TxStream && this.getId() == other.getId());
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

	public List<byte[]> getChunks() throws InputFormatException {
        return chunks.get(() -> {
            try {
                return List.copyOf(loader.getChunksAtOffsets(fileOffsets));
            } catch (InputFormatException e) {
                throw new IllegalStateException("Cannot read stream " + getFullName(), e);
            }
        });
    }

    public void loadStream() {
        parsed.get(() -> {
            try {
                int blockId = 0;
                for (byte[] chunk : getChunks()) loader.parseTx(this, blockId++, chunk);
                return Boolean.TRUE;
            } catch (InputFormatException | IOException e) {
                throw new IllegalStateException("Cannot parse stream " + getFullName(), e);
            }
        });
    }

    @Override
    public IEventList getEvents() {
        loadStream();
        return events;
    }

	@Override
	public DirectionType getDirection() {
		return DirectionType.IMPLICIT;
	}
}
