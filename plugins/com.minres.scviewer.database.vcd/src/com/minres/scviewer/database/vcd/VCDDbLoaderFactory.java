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
package com.minres.scviewer.database.vcd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformDbLoaderFactory;

/**
 * The Class VCDDb.
 */
public class VCDDbLoaderFactory implements IWaveformDbLoaderFactory {
	/**
	 * Checks if is gzipped.
	 *
	 * @param f the f
	 * @return true, if is gzipped
	 */
	private static boolean isGzipped(File f) {
		try (InputStream is = new FileInputStream(f)) {
			byte [] signature = new byte[2];
			int nread = is.read( signature ); //read the gzip signature
			return nread == 2 && signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b;
		}
		catch (IOException e) {
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
		if(!inputFile.isDirectory() || inputFile.exists()) {
			String name = inputFile.getName();
			if(!(name.endsWith(".vcd") ||
					name.endsWith(".vcdz") ||
					name.endsWith(".vcdgz")  ||
					name.endsWith(".vcd.gz")) )
				return false;
			boolean gzipped = isGzipped(inputFile);
			try(InputStream stream = gzipped ? new GZIPInputStream(new FileInputStream(inputFile)) : new FileInputStream(inputFile)){
				byte[] buffer = new byte[8];
				if (stream.read(buffer, 0, buffer.length) == buffer.length) {
					return buffer[0]=='$';
				}
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}


	@Override
	public IWaveformDbLoader getLoader() {
		return new VCDDbLoader();
	}
}
