/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
