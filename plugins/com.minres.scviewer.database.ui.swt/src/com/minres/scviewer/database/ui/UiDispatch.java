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
