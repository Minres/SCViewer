package com.minres.scviewer.database.ui.swt.internal.slider;

import org.eclipse.swt.widgets.Display;

public class ActionTimer implements Runnable {

	public static final int INITIAL_DELAY = 300;
	public static final int FAST_DELAY = 50;

	private final Display display;
	private final TimerAction timerAction;

	public interface TimerAction extends Runnable {
		boolean isEnabled();
	}

	public ActionTimer( TimerAction timerAction, Display display ) {
		this.display = display;
		this.timerAction = timerAction;
	}

	public void activate() {
		if( timerAction.isEnabled() ) {
			display.timerExec( INITIAL_DELAY, this );
		}
	}

	@Override
	public void run() {
		if( timerAction.isEnabled() ) {
			timerAction.run();
			display.timerExec( FAST_DELAY, this );
		}
	}
}