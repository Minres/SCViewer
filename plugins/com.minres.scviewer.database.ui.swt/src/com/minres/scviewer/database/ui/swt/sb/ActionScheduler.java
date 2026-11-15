/*******************************************************************************
 * Copyright (c) 2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public void schedule( int delay ) {
    display.timerExec( delay, action );
  }
}