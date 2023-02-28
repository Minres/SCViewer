package com.minres.scviewer.database.ui;

import java.util.ArrayList;
import java.util.List;

import com.minres.scviewer.database.BlankWaveform;

public class TrackEntryGroup extends TrackEntry {

	public List<TrackEntry> waveforms = new ArrayList<>();
	
	public Boolean is_open = true;
	
	public TrackEntryGroup(TrackEntry[] waveform, IWaveformStyleProvider styleProvider) {
		super(new BlankWaveform(), styleProvider);
		for (TrackEntry iWaveform : waveform) {
			waveforms.add(iWaveform);
		}
	}

}
