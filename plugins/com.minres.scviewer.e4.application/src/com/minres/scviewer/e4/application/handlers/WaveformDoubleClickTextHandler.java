/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
 
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.CombinedBitVectorWaveform;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.TextInputDialog;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class WaveformDoubleClickTextHandler {

    private TrackEntry editableTrack(MPart part) {
        if (part == null || !(part.getObject() instanceof WaveformViewer viewer)) return null;
        if (!(viewer.getSelection() instanceof IStructuredSelection selection) || selection.size() != 1
                || !(selection.getFirstElement() instanceof TrackEntry track)) return null;
        return track.isGroup() || track.waveform instanceof CombinedBitVectorWaveform ? track : null;
    }

    @CanExecute
    public Boolean canExecute(MPart activePart) {
        return editableTrack(activePart) != null;
    }

    @Execute
    public void execute(Shell shell, EPartService partService) {
        MPart part = partService.getActivePart();
        TrackEntry track = editableTrack(part);
        if (track == null) return;
        String text = requestText(shell, track);
        if (text == null || (track.waveform instanceof CombinedBitVectorWaveform && text.isBlank())) return;
        track.waveform.setName(text);
        ((WaveformViewer) part.getObject()).update();
    }

    /** A null result represents cancellation. */
    protected String requestText(Shell shell, TrackEntry track) {
        TextInputDialog dialog = new TextInputDialog(shell) {
            protected String validateText(String text) {
                return track.waveform instanceof CombinedBitVectorWaveform && text.isBlank()
                        ? "Enter a name for the combined vector" : null;
            }
        };
        dialog.create();
        dialog.setTitle(track.isGroup() ? "Change Group Text" : "Change Combined Vector Text");
        dialog.setText(track.waveform.getName());
        return dialog.open() == Window.OK ? dialog.getText() : null;
    }
}
