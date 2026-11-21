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

public class MouseDownActionTimer implements Runnable {

  public static final int INITIAL_DELAY = 300;
  public static final int FAST_DELAY = 50;

  private final ActionScheduler scheduler;
  private boolean active;
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
      active = true;
      scheduler.schedule( INITIAL_DELAY );
    }
  }

  public void cancel() { active = false; scheduler.cancel(); }

  @Override
  public void run() {
    if( active && mouseClick.isArmed() && timerAction.isEnabled() ) {
      timerAction.run();
      if (active && timerAction.isEnabled()) scheduler.schedule(FAST_DELAY);
    }
  }
}