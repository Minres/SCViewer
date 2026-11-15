/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
