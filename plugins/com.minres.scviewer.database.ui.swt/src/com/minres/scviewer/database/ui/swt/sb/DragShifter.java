package com.minres.scviewer.database.ui.swt.sb;

import static com.minres.scviewer.database.ui.swt.sb.Direction.HORIZONTAL;
import static com.minres.scviewer.database.ui.swt.sb.ShiftData.calculateSelectionRange;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

import com.minres.scviewer.database.ui.swt.sb.DragControl.DragAction;

final class DragShifter implements DragAction {

  private final FlatScrollBar scrollBar;
  private final int buttonLength;

  public DragShifter( FlatScrollBar scrollBar, int buttonLength ) {
    this.scrollBar = scrollBar;
    this.buttonLength = buttonLength;
  }

  @Override
  public void start() {
    scrollBar.notifyListeners( SWT.DRAG );
  }

  @Override
  public void run( int startX, int startY, int currentX, int currentY ) {
    ShiftData shiftData = newShiftData( startX, startY, currentX, currentY );
    if( shiftData.canShift() ) {
      int selectionRange = calculateSelectionRange( scrollBar );
      int selectionDelta = shiftData.calculateSelectionDelta( selectionRange );
      int selection = scrollBar.getSelection() + selectionDelta;
      scrollBar.setSelectionInternal( selection, SWT.DRAG );
    }
  }

  @Override
  public void end() {
    scrollBar.notifyListeners( SWT.NONE );
  }

  private ShiftData newShiftData( int startX, int startY, int currentX, int currentY ) {
    ShiftData result;
    if( scrollBar.direction == HORIZONTAL ) {
      result = new ShiftData( buttonLength, getScrollBarSize().x, getDragSize().x, currentX - startX );
    } else {
      result = new ShiftData( buttonLength, getScrollBarSize().y, getDragSize().y, currentY - startY );
    }
    return result;
  }

  private Point getScrollBarSize() {
    return scrollBar.getSize();
  }

  private Point getDragSize() {
    return scrollBar.drag.getControl().getSize();
  }
}