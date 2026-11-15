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

public class MouseDownActionTimer implements Runnable {

  public static final int INITIAL_DELAY = 300;
  public static final int FAST_DELAY = 50;

  private final ActionScheduler scheduler;
  private final TimerAction timerAction;
  private final ButtonClick mouseClick;

  public interface TimerAction extends Runnable {
    boolean isEnabled();
  }

  public MouseDownActionTimer( TimerAction timerAction, ButtonClick mouseClick, Display display ) {
    this.scheduler = new ActionScheduler( display, this );
    this.timerAction = timerAction;
    this.mouseClick = mouseClick;
  }

  public void activate() {
    if( timerAction.isEnabled() ) {
      scheduler.schedule( INITIAL_DELAY );
    }
  }

  @Override
  public void run() {
    if( mouseClick.isArmed() && timerAction.isEnabled() ) {
      timerAction.run();
      scheduler.schedule( FAST_DELAY );
    }
  }
}