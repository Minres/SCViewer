/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

/**
 * The Interface IDerivedWaveform.
 */
public interface IDerivedWaveform extends IWaveform {

	/**
	 * Adds the source waveform.
	 *
	 * @param waveform the waveform
	 */
	void addSourceWaveform(IWaveform waveform);
}
