/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020-2026 MINRES Technologies GmbH
 *
 * SPDX-License-Identifier: EPL-1.0
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

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;

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
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		FileType fType = TextDbLoader.getFileType(inputFile);
		try (InputStream is = new FileInputStream(inputFile)) {
			InputStream plainIs = fType==FileType.GZIP ? new GZIPInputStream(is) : fType==FileType.LZ4? new FramedLZ4CompressorInputStream(is) : is;
			byte[] buffer = new byte[x.length];
			int readCnt = plainIs.read(buffer, 0, x.length);
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
		return new TextDbLoader();
	}
}
