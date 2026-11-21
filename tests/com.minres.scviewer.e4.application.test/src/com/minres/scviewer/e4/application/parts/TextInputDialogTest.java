/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

public class TextInputDialogTest {
    @Test public void invalidTextKeepsDialogOpenAndValidTextIsAccepted() {
        assumeTrue("GTK integration test requires DISPLAY", !"gtk".equals(SWT.getPlatform())
                || System.getenv("DISPLAY") != null);
        Display display = new Display();
        Shell parent = new Shell(display);
        TextInputDialog dialog = new TextInputDialog(parent) {
            @Override protected String validateText(String text) { return text.isBlank() ? "Enter a name" : null; }
        };
        try {
            dialog.create();
            dialog.setText(" ");
            dialog.okPressed();
            assertFalse(dialog.getShell().isDisposed());
            assertEquals("Enter a name", dialog.getErrorMessage());
            dialog.setText("vector label");
            dialog.okPressed();
            assertEquals(org.eclipse.jface.window.Window.OK, dialog.getReturnCode());
            assertEquals("vector label", dialog.getText());
        } finally {
            if (dialog.getShell() != null && !dialog.getShell().isDisposed()) dialog.close();
            parent.dispose();
            display.dispose();
        }
    }
}
