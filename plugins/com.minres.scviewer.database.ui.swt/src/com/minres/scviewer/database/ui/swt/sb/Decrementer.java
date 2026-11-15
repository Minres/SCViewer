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

import org.eclipse.swt.SWT;

import com.minres.scviewer.database.ui.swt.sb.ClickControl.ClickAction;

class Decrementer implements ClickAction {

  private final FlatScrollBar scrollBar;

  Decrementer( FlatScrollBar scrollBar ) {
    this.scrollBar = scrollBar;
  }

  @Override
  public void run() {
    int selection = scrollBar.getSelection() - scrollBar.getIncrement();
    scrollBar.setSelectionInternal( selection, SWT.ARROW_UP );
  }

  @Override
  public void setCoordinates( int x, int y ) {
  }
}