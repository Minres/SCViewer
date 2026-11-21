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

import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.fst.FstLibrary.HierAttr;
import com.minres.scviewer.database.fst.FstLibrary.HierScope;
import com.minres.scviewer.database.fst.FstLibrary.HierType;
import com.minres.scviewer.database.fst.FstLibrary.HierVar;
import com.minres.scviewer.database.fst.FstLibrary.ValueChangeCallback;
import com.sun.jna.Pointer;

class FstFileParser {
	long currentTime;
	final File file;
	Pointer fst;
	
	public FstFileParser(File file) {
		this.file=file;
	}

	public synchronized boolean open(IFstDatabaseBuilder builder) {
	    fst = FstLibrary.fstReaderOpen(file.getAbsolutePath());
	    if(fst != null && !fst.equals(Pointer.NULL)) {
	    	// String version = FstLibrary.fstReaderGetVersionString(fst);
	    	long endTime = FstLibrary.fstReaderGetEndTime(fst);
	    	byte timeScale = FstLibrary.fstReaderGetTimescale(fst);
	    	builder.setMaxTime(endTime, -timeScale);
	    	FstLibrary.fstReaderIterateHierRewind(fst);
	    	Pointer p = FstLibrary.fstReaderIterateHier(fst);
	    	while(p!=null && !p.equals(Pointer.NULL)) {
	    		int hierType = FstLibrary.getHierType(p);
		    	HierType type = HierType.values()[hierType];
	    		switch(type) {
	    		case HT_SCOPE:
	    			HierScope scope =  new HierScope();
	    			FstLibrary.getHierScope(p, scope);
	    			builder.enterModule(scope.name);
	    			break;
	    		case HT_UPSCOPE:
	    			builder.exitModule();
	    			break;
	    		case HT_VAR:
	    			HierVar v =  new HierVar();
	    			FstLibrary.getHierVar(p, v);
	    			builder.newNet(v.name,  v.handle,  v.length, v.direction, v.is_alias!=0);
	    			break;
	    		case HT_ATTRBEGIN:
	    			HierAttr attr =  new HierAttr();
	    			FstLibrary.getHierAttr(p, attr);
	    			break;
	    		case HT_ATTREND:
	    			break;
	    		case HT_TREEBEGIN:
	    			break;
	    		case HT_TREEEND:
	    			break;
				default:
					break;
	    		}
		    	p = FstLibrary.fstReaderIterateHier(fst);
	    	}
	    	return true;
	    } else
	    	return false;
	}

	public synchronized void getValueChanges(final int id, final int width, long timeScale, final EventList values) {
		if (fst == null) throw new IllegalStateException("FST reader is closed");
		FstLibrary.fstReaderClrFacProcessMaskAll(fst);
		FstLibrary.fstReaderSetFacProcessMask(fst, id);
		FstLibrary.iterateValueChanges(fst, new ValueChangeCallback() {
			@Override
			public void callback(long time, int facidx, String value) {
				values.put(time*timeScale, BitVector.fromString(width, value));
			}
		});
	}
	public synchronized void close() {
		if (fst != null) {
			FstLibrary.fstReaderClose(fst);
			fst = null;
		}
	}

}
