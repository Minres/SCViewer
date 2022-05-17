/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformDbLoaderFactory;
import com.minres.scviewer.database.text.TextDbLoader.FileType;

/**
 * The Class TextDbLoader.
 */
public class TextDbLoaderFactory implements IWaveformDbLoaderFactory {

	/** The Constant x. */
	static final byte[] x = "scv_tr_stream".getBytes();

	/**
	 * Checks if f is gzipped.
	 *
	 * @param f the f
	 * @return true, if is gzipped
	 */
	private static boolean isGzipped(File f) {
		try (InputStream is = new FileInputStream(f)) {
			byte[] signature = new byte[2];
			int nread = is.read(signature); // read the gzip signature
			return nread == 2 && signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		try (InputStream is = new FileInputStream(inputFile)) {
			byte[] signature = new byte[4];
			int nread = is.read(signature); // read the gzip signature
			if(nread >= 2 && 
					signature[0] == (byte) 0x1f && 
					signature[1] == (byte) 0x8b)
				return true;
			else if(nread>=4 && 
					signature[0] == (byte) 0x04 && 
					signature[1] == (byte) 0x22 && 
					signature[2] == (byte) 0x4d && 
					signature[3] == (byte) 0x18)
				return true;
			else
				return true;
		} catch (IOException e) {}
		return false;
	}

	@Override
	public IWaveformDbLoader getLoader() {
		return new TextDbLoader();
	}
}
