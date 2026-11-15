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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

class FlatScrollBarLayout extends Layout {

  private final Direction direction;

  public FlatScrollBarLayout( Direction orientation ) {
    this.direction = orientation;
  }

  @Override
  protected void layout( Composite composite, boolean flushCache ) {
  }

  @Override
  protected Point computeSize( Composite composite, int wHint, int hHint, boolean flushCache ) {
    return direction.computeSize( composite, wHint, hHint, flushCache );
  }
}