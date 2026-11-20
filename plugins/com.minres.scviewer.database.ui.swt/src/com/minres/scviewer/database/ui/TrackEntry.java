/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
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
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;

public class TrackEntry {
	public enum HierState { 
		NONE, CLOSED, OPENED
	}

	public enum ValueDisplay {
		DEFAULT, BINARY, SIGNED, UNSIGNED

	}

	public enum WaveDisplay {
		DEFAULT, STEP_WISE, CONTINOUS
	}

	IWaveformStyleProvider styleProvider = null;

	final public IWaveform waveform;
	private final boolean bitVector;

	public int vOffset=0;
	
	public int height=0;

	public boolean selected=false;
	
	public HierState hierState=HierState.NONE;

	public List<TrackEntry> waveforms = new ArrayList<>();

	public String currentValue="";
	
	public ValueDisplay valueDisplay = ValueDisplay.DEFAULT;
	
	public WaveDisplay waveDisplay = WaveDisplay.DEFAULT;
	
	public TrackEntry() {
		this.waveform = null;
		this.bitVector = false;
		this.styleProvider=null;
	}

	public TrackEntry(IWaveform waveform, IWaveformStyleProvider styleProvider) {
		this.waveform = waveform;
		this.bitVector = isBitVector(waveform);
		this.styleProvider=styleProvider;
		this.hierState = (waveform!=null && waveform.getChildNodes().size()>0)?HierState.CLOSED:HierState.NONE;
		if (isBitVector()) this.hierState = HierState.CLOSED;
	}

	public TrackEntry(TrackEntry[] waveform, IWaveformStyleProvider styleProvider) {
		this(new EmptyWaveform(), styleProvider);
		this.hierState = HierState.CLOSED;
		for (TrackEntry iWaveform : waveform) {
			waveforms.add(iWaveform);
		}
	}

	/** A group retains its header when expanded. Children are ordinary tracks or groups. */
	public static TrackEntry group(String name, List<TrackEntry> children, IWaveformStyleProvider styles) {
		TrackEntry group = new TrackEntry(children.toArray(new TrackEntry[0]), styles);
		group.waveform.setName(name);
		group.hierState = HierState.OPENED;
		return group;
	}

	public boolean isGroup() {
		return waveform instanceof EmptyWaveform && waveform.getType() == WaveformType.EMPTY && hierState != HierState.NONE;
	}

	public boolean isBitVector() {
		return bitVector;
	}

    private static boolean isBitVector(IWaveform waveform) {
        return waveform != null && waveform.getWidth() > 1 && waveform.isBitVector();
    }

	public boolean isExpandable() {
		return isGroup() || isBitVector();
	}

	/** Create bit tracks only on the first expansion, preserving their display settings on collapse. */
	public void setExpanded(boolean expanded) {
		if (!isExpandable()) return;
		if (expanded && isBitVector() && waveforms.isEmpty()) {
			for (int bit = waveform.getWidth() - 1; bit >= 0; bit--)
				waveforms.add(new TrackEntry(new BitWaveform(waveform, bit), styleProvider));
		}
		hierState = expanded ? HierState.OPENED : HierState.CLOSED;
	}

	/** Returns the actual sibling list owning this entry, including collapsed groups. */
	public static List<TrackEntry> siblingsOf(List<TrackEntry> roots, TrackEntry track) {
		if (roots.stream().anyMatch(entry -> entry == track)) return roots;
		for (TrackEntry entry : roots) {
			if (!entry.isExpandable()) continue;
			List<TrackEntry> siblings = siblingsOf(entry.waveforms, track);
			if (siblings != null) return siblings;
		}
		return null;
	}

	/** Group only siblings, so selecting a group and its descendant cannot create a cycle. */
	public static List<TrackEntry> groupingSiblings(List<TrackEntry> roots, List<TrackEntry> selected) {
		if (selected.isEmpty()) return null;
		List<TrackEntry> siblings = siblingsOf(roots, selected.get(0));
		return siblings != null && selected.stream().allMatch(track -> siblingsOf(roots, track) == siblings)
				? siblings : null;
	}

	public static TrackEntry groupTracks(List<TrackEntry> roots, List<TrackEntry> selected,
			String name, IWaveformStyleProvider styles) {
		List<TrackEntry> siblings = groupingSiblings(roots, selected);
		if (siblings == null) return null;
		List<TrackEntry> members = siblings.stream()
				.filter(entry -> selected.stream().anyMatch(track -> track == entry)).toList();
		int index = siblings.indexOf(members.get(0));
		TrackEntry group = group(name, members, styles);
		siblings.removeAll(members);
		siblings.add(index, group);
		return group;
	}

	public static boolean ungroupTracks(List<TrackEntry> roots, TrackEntry group) {
		List<TrackEntry> siblings = siblingsOf(roots, group);
		if (!group.isGroup() || siblings == null) return false;
		int index = siblings.indexOf(group);
		siblings.remove(index);
		siblings.addAll(index, group.waveforms);
		return true;
	}

	/** Remove selected entries from their owning lists, leaving empty group headers intact. */
	public static void removeTracks(List<TrackEntry> roots, List<TrackEntry> selected) {
		for (TrackEntry track : new ArrayList<>(selected)) {
			track.selected = false;
			List<TrackEntry> siblings = siblingsOf(roots, track);
			if (siblings == null) continue;
			for (int index = 0; index < siblings.size(); index++) {
				if (siblings.get(index) == track) {
					siblings.remove(index);
					break;
				}
			}
		}
	}

	public static List<TrackEntry> visibleTracks(List<TrackEntry> roots) {
		List<TrackEntry> result = new ArrayList<>();
		for (TrackEntry entry : roots) {
			result.add(entry);
			if (entry.hierState == HierState.OPENED)
				result.addAll(visibleTracks(entry.waveforms));
		}
		return result;
	}

	@Override
    public boolean equals(Object obj) {
		if(obj instanceof TrackEntry){
			TrackEntry o = (TrackEntry) obj;
			return waveform==o.waveform && vOffset==o.vOffset;
		}
		return false;
	}
	
}
