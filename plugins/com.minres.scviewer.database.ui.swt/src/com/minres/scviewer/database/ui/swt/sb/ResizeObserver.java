/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;

class ResizeObserver extends ControlAdapter {

  private final FlatScrollBar flatScrollBar;

  public ResizeObserver( FlatScrollBar flatScrollBar ) {
    this.flatScrollBar = flatScrollBar;
  }

  @Override
  public void controlResized( ControlEvent event ) {
    flatScrollBar.layout();
    flatScrollBar.moveAbove( null );
  }
}