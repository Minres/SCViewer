/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

/** Enqueue against a captured display, tolerating disposal during submission. */
public final class UiDispatch {
    private UiDispatch() { }
    public static void async(Display display, Runnable action) {
        if (display.isDisposed()) return;
        try { display.asyncExec(() -> { if (!display.isDisposed()) action.run(); }); }
        catch (SWTException e) { if (e.code != SWT.ERROR_DEVICE_DISPOSED) throw e; }
    }
}
