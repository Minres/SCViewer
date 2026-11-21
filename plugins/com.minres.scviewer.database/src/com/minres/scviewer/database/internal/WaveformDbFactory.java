/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package com.minres.scviewer.database.internal;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;

/**
 * A factory for creating WaveformDb objects.
 *
 * @author eyck
 */
public class WaveformDbFactory implements IWaveformDbFactory {

	/**
	 * Gets the database.
	 *
	 * @return the database
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.IWaveformDbFactory#getDatabase()
	 */
	@Override
	public IWaveformDb getDatabase() {
		return new WaveformDb();
	}

}
