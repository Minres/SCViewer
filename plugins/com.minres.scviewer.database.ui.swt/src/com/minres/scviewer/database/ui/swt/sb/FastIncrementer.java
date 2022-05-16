package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.minres.scviewer.database.ui.swt.sb.ClickControl.ClickAction;

class FastIncrementer implements ClickAction {

  private final FlatScrollBar scrollBar;

  private Point mouse;

  FastIncrementer( FlatScrollBar scrollBar ) {
    this.scrollBar = scrollBar;
  }

  @Override
  public void run() {
    Rectangle drag = getDragBounds();
    if( mouse.x > drag.x + drag.width || mouse.y > drag.y + drag.height ) {
      int selection = scrollBar.getSelection() + scrollBar.getPageIncrement();
      scrollBar.setSelectionInternal( selection, SWT.PAGE_DOWN );
    }
  }

  @Override
  public void setCoordinates( int x, int y ) {
    mouse = getMouseLocation( x, y );
  }

  private Point getMouseLocation(int x, int y) {
    return Display.getCurrent().map( scrollBar.downFast.getControl(), null, x, y );
  }

  private Rectangle getDragBounds() {
    Rectangle dragBounds = scrollBar.drag.getControl().getBounds();
    return Display.getCurrent().map( scrollBar, null, dragBounds );
  }
}