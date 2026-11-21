/*******************************************************************************
 * Copyright (c) 2022-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.internal.slider;

import org.eclipse.swt.widgets.Display;

public class ActionTimer implements Runnable {

	public static final int INITIAL_DELAY = 300;
	public static final int FAST_DELAY = 50;

	private final Display display;
	private final TimerAction timerAction;
    private boolean active;

	public interface TimerAction extends Runnable {
		boolean isEnabled();
	}

	public ActionTimer( TimerAction timerAction, Display display ) {
		this.display = display;
		this.timerAction = timerAction;
	}

	public void activate() {
		if (!display.isDisposed() && timerAction.isEnabled()) {
            active = true;
			display.timerExec( INITIAL_DELAY, this );
		}
	}

    public void cancel() {
        active = false;
        if (!display.isDisposed()) display.timerExec(-1, this);
    }

	@Override
	public void run() {
		if (active && !display.isDisposed() && timerAction.isEnabled()) {
            timerAction.run();
            if (active && !display.isDisposed() && timerAction.isEnabled()) display.timerExec(FAST_DELAY, this);
		}
	}
}