/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/

package com.minres.scviewer.database.ui;

import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.EmptyWaveform;

public class TrackEntryGroup extends TrackEntry {

	public List<TrackEntry> waveforms = new ArrayList<>();
	
	public Boolean is_open = true;
	
	public TrackEntryGroup(TrackEntry[] waveform, IWaveformStyleProvider styleProvider) {
		super(new EmptyWaveform(), styleProvider);
		for (TrackEntry iWaveform : waveform) {
			waveforms.add(iWaveform);
		}
	}

}
