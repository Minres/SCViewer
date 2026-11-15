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