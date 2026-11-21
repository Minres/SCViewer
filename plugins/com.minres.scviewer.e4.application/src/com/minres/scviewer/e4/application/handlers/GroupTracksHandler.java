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
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class GroupTracksHandler {
    static WaveformViewer activeViewer(EPartService parts) {
        MPart part = parts == null ? null : parts.getActivePart();
        return part != null && part.getObject() instanceof WaveformViewer viewer ? viewer : null;
    }

    @CanExecute
    public boolean canExecute(EPartService parts) {
        WaveformViewer viewer = activeViewer(parts);
        return viewer != null && viewer.canGroupSelectedTracks();
    }

    @Execute
    public void execute(EPartService parts, Shell shell) {
        WaveformViewer viewer = activeViewer(parts);
        if (viewer == null || !viewer.canGroupSelectedTracks()) return;
        String name = requestGroupName(shell);
        if (name != null) viewer.groupSelectedTracks(name);
    }

    protected String requestGroupName(Shell shell) {
        InputDialog dialog = new InputDialog(shell, "Group tracks", "Group name", "Group",
                name -> name.isBlank() ? "Enter a group name" : null);
        return dialog.open() == Window.OK ? dialog.getValue() : null;
    }
}
