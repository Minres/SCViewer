/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.binary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformEvent;
import com.minres.scviewer.database.RelationType;

public class BinaryDbLoader implements IWaveformDbLoader {

	
	private List<RelationType> usedRelationsList = new ArrayList<>();

	private IWaveformDb db;
	
		
	public BinaryDbLoader() {
	}

	@Override
	public Long getMaxTime() {
		return 0L;
	}

	@Override
	public List<IWaveform<? extends IWaveformEvent>> getAllWaves() {
		List<IWaveform<? extends IWaveformEvent>> streams=new ArrayList<IWaveform<? extends IWaveformEvent>>();
		return streams;
	}

	@Override
	public boolean load(IWaveformDb db, File file) throws Exception {
		return false;
	}
	
	@Override
	public Collection<RelationType> getAllRelationTypes(){
		return usedRelationsList;
	}

}
