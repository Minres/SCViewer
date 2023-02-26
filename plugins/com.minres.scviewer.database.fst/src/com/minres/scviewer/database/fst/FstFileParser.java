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

import java.io.*;
import java.text.ParseException;
import java.util.*;

import com.minres.scviewer.database.BitValue;
import com.minres.scviewer.database.BitVector;
import com.sun.jna.Pointer;

class FstFileParser {
	long currentTime;
	final File file;
	
	public FstFileParser(File file) {
		this.file=file;
	}

	public boolean load(IFstDatabaseBuilder builder) {
	    Pointer ctx = FstLibrary.fstReaderOpen(file.getAbsolutePath());
	    if(!ctx.equals(Pointer.NULL)) {
	    	String version = FstLibrary.fstReaderGetVersionString(ctx);
	    	System.out.println(version);
	    	FstLibrary.fstReaderClose(ctx);
	    }
		return false;
	}

}
