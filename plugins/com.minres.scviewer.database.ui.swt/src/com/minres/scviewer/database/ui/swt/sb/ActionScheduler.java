/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.widgets.Display;

public class ActionScheduler {

  private final Display display;
  private final Runnable action;

  public ActionScheduler( Display display, Runnable action ) {
    this.display = display;
    this.action = action;
  }

  public void cancel() { if (!display.isDisposed()) display.timerExec(-1, action); }

  public void schedule( int delay ) {
    if (!display.isDisposed()) display.timerExec(delay, action);
  }
}