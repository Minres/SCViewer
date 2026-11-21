/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class ButtonClick {

  public static final int LEFT_BUTTON = 1;

  private boolean armed;

  public boolean isArmed() {
    return armed;
  }

  public void arm( MouseEvent event ) {
    if( event.button == LEFT_BUTTON ) {
      armed = true;
    }
  }

  public void disarm() {
    armed = false;
  }

  public void trigger( MouseEvent event, Runnable action ) {
    try {
      doTrigger( event, action );
    } finally {
      disarm();
    }
  }

  private void doTrigger( MouseEvent event, Runnable action ) {
    if( armed && inRange( event ) ) {
      action.run();
    }
  }

  private static boolean inRange( MouseEvent event ) {
    Point size = ( ( Control )event.widget ).getSize();
    return event.x >= 0 && event.x <= size.x && event.y >= 0 && event.y <= size.y;
  }
}