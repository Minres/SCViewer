/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.io.File;

/**
 * A factory for creating IWaveformDb objects.
 */
public interface IWaveformDbLoaderFactory {

	/**
	 * Check if the loader produced by this factory can load the given file.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	public boolean canLoad(File inputFile);
	/**
	 * Gets the database.
	 *
	 * @return the database
	 */
	IWaveformDbLoader getLoader();
}
