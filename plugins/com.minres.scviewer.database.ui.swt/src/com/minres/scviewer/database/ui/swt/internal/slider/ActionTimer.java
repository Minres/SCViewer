/*******************************************************************************
 * Copyright (c) 2022 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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