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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformDbLoaderFactory;

/**
 * The Class TextDbLoader.
 */
public class FtrDbLoaderFactory implements IWaveformDbLoaderFactory {

	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	/** The Constant x. */
	static final byte[] x =  hexStringToByteArray("d9d9f79f");

	/**
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		try (InputStream is = new FileInputStream(inputFile)) {
			byte[] buffer = new byte[x.length];
			int readCnt = is.read(buffer, 0, x.length);
			if (readCnt == x.length) {
				for (int i = 0; i < x.length; i++)
					if (buffer[i] != x[i]) return false;
			}
			return true;
		} catch (IOException e) {}
		return false;
	}

	@Override
	public IWaveformDbLoader getLoader() {
		return new FtrDbLoader();
	}
}
