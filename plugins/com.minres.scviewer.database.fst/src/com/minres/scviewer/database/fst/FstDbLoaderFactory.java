/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.fst;

import java.io.File;

import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformDbLoaderFactory;

/**
 * The Class VCDDb.
 */
public class FstDbLoaderFactory implements IWaveformDbLoaderFactory {
	/**
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		if(!inputFile.isDirectory() || inputFile.exists()) {
			String name = inputFile.getName();
			return name.endsWith(".fst");
		}
		return false;
	}


	@Override
	public IWaveformDbLoader getLoader() {
		return new FstDbLoader();
	}
}
