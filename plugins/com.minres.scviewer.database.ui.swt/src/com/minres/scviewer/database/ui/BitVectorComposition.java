/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

import java.util.List;
import com.minres.scviewer.database.CombinedBitVectorWaveform.SourceBit;
import com.minres.scviewer.database.WaveformType;

/** Selection and source normalization shared by the combine action and viewer. */
public final class BitVectorComposition {
    private BitVectorComposition() { }

    public static boolean isSingleBit(TrackEntry track) {
        if (track.waveform == null || track.waveform.getType() != WaveformType.SIGNAL
                || track.waveform.getWidth() != 1) return false;
        return track.waveform.isBitVector();
    }

    public static List<TrackEntry> selectedBits(List<TrackEntry> roots, List<TrackEntry> selection) {
        List<TrackEntry> ordered = TrackEntry.visibleTracks(roots).stream()
                .filter(track -> selection.stream().anyMatch(selected -> selected == track)).toList();
        return ordered.size() >= 2 && ordered.size() == selection.size() && ordered.stream().allMatch(BitVectorComposition::isSingleBit)
                ? ordered : List.of();
    }

    public static List<SourceBit> sources(List<TrackEntry> tracks) {
        if (tracks.size() < 2 || !tracks.stream().allMatch(BitVectorComposition::isSingleBit))
            throw new IllegalArgumentException("Select at least two single-bit signals");
        return tracks.stream().map(track -> track.waveform instanceof BitWaveform bit
                ? new SourceBit(bit.getSource(), bit.getBitIndex()) : new SourceBit(track.waveform, 0)).toList();
    }
}
