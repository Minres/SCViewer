/*******************************************************************************
 * Copyright (c) 2021-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.SWT;

import com.minres.scviewer.database.ui.swt.sb.ClickControl.ClickAction;

class Incrementer implements ClickAction {

  private final FlatScrollBar scrollBar;

  Incrementer( FlatScrollBar scrollBar ) {
    this.scrollBar = scrollBar;
  }

  @Override
  public void run() {
    int selection = scrollBar.getSelection() + scrollBar.getIncrement();
    scrollBar.setSelectionInternal( selection, SWT.ARROW_DOWN );
  }

  @Override
  public void setCoordinates( int x, int y ) {
  }
}