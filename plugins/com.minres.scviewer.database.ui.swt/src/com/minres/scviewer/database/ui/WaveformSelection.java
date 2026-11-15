package com.minres.scviewer.database.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import com.minres.scviewer.database.tx.ITx;

/** The selected transaction and tracks, with JFace conversion at the UI boundary. */
public record WaveformSelection(ITx transaction, List<TrackEntry> tracks) {
    public WaveformSelection {
        tracks = tracks.stream().filter(java.util.Objects::nonNull).toList();
    }

    public static WaveformSelection from(ISelection selection) {
        if (!(selection instanceof IStructuredSelection structured)) return new WaveformSelection(null, List.of());
        ITx transaction = null;
        List<TrackEntry> tracks = new ArrayList<>();
        for (Object element : structured.toArray()) {
            if (element instanceof ITx tx && transaction == null) transaction = tx;
            if (element instanceof TrackEntry track) tracks.add(track);
        }
        return new WaveformSelection(transaction, tracks);
    }

    public TrackEntry track() {
        if (transaction == null) return tracks.isEmpty() ? null : tracks.get(0);
        return tracks.stream().filter(t -> t.waveform == transaction.getStream()
                || t.waveform == transaction.getGenerator()).findFirst().orElse(null);
    }

    public IStructuredSelection toSelection() {
        List<Object> elements = new ArrayList<>();
        if (transaction != null) elements.add(transaction);
        TrackEntry primary = track();
        if (primary != null) elements.add(primary);
        for (TrackEntry track : tracks) if (track != primary) elements.add(track);
        return new StructuredSelection(elements);
    }

    public static IStructuredSelection of(ITx transaction, TrackEntry track) {
        return new WaveformSelection(transaction, track == null ? List.of() : List.of(track)).toSelection();
    }
}
