/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

/**
 * The Enum WaveformType.
 */
public enum WaveformType {

	/** The signal. */
	SIGNAL,
	/** The transaction stream. */
	STREAM,
	/** The transaction generator. */
	GENERATOR,
	/** The filter. */
	FILTER,
	/** The blank line. */
	EMPTY;

    /** Whether this waveform contains transactions, in a stream or a generator. */
    public boolean isTransaction() {
        return this == STREAM || this == GENERATOR;
    }
}
