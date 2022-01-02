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