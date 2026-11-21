/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class UngroupTracksHandler {
    @CanExecute
    public boolean canExecute(EPartService parts) {
        WaveformViewer viewer = GroupTracksHandler.activeViewer(parts);
        return viewer != null && viewer.canUngroupSelectedTracks();
    }

    @Execute
    public void execute(EPartService parts) {
        WaveformViewer viewer = GroupTracksHandler.activeViewer(parts);
        if (viewer != null && viewer.canUngroupSelectedTracks()) viewer.ungroupSelectedTracks();
    }
}
