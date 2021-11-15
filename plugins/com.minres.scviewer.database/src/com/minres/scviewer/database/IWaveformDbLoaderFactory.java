/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
