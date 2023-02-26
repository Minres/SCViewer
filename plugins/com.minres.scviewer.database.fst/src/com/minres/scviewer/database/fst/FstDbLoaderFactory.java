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
