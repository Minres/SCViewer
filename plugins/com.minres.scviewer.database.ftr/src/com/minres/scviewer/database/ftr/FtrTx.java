/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.io.Serializable;
import java.util.List;

import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.LazyValue;
import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class ScvTx.
 */
class FtrTx implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -855200240003328221L;

	/** The id. */
	final long id;

	/** The generator id. */
	final long generatorId;

	/** The stream id. */
	final long streamId;

	/** The begin time. */
	long beginTime;

	/** The end time. */
	long endTime;

	/** The attributes. */
	private final LazyValue<List<ITxAttribute>> attributes = new LazyValue<>();

	final long blockId;

	final long blockOffset;

	/**
	 * Instantiates a new scv tx.
	 *
	 * @param id          the id
	 * @param streamId    the stream id
	 * @param generatorId the generator id
	 * @param begin       the begin
	 */
	FtrTx(long id, long streamId, long generatorId, long begin, long end, long blockId, long blockOffset) {
		this.id = id;
		this.streamId = streamId;
		this.generatorId = generatorId;
		this.beginTime = begin;
		this.endTime = end;
		this.blockId=blockId;
		this.blockOffset=blockOffset;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	Long getId() {
		return id;
	}

	public List<ITxAttribute> getAttributes(FtrDbLoader loader) {
		return attributes.get(() -> {
            try {
                TxStream stream = loader.txStreams.get(streamId);
                byte[] chunk = stream.getChunks().get((int) blockId);
                return List.copyOf(loader.parseAtrributes(chunk, blockOffset));
            } catch (InputFormatException e) {
                throw new IllegalStateException("Cannot read attributes for transaction " + id, e);
            }
        });
    }
}
