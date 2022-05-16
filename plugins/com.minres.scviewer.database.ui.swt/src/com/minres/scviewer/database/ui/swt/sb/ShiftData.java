package com.minres.scviewer.database.ui.swt.sb;

import static com.minres.scviewer.database.ui.swt.sb.ComponentDistribution.divide;

class ShiftData {

  private final int slidePixels;
  private final int movedPixels;
  private final int buttonLength;

  ShiftData( int buttonLength, int scrollBarPixels, int dragPixels, int movedPixels ) {
    this.buttonLength = buttonLength;
    this.slidePixels = calculateSlidePixels( scrollBarPixels, dragPixels );
    this.movedPixels = movedPixels;
  }

  boolean canShift( ) {
    return slidePixels > 0;
  }

  int calculateSelectionDelta( int selectionRange ) {
    return divide( movedPixels * selectionRange, slidePixels );
  }

  static int calculateSelectionRange( FlatScrollBar scrollBar ) {
    return scrollBar.getMaximum() - scrollBar.getMinimum() - scrollBar.getThumb();
  }

  private int calculateSlidePixels( int scrollBarPixels, int dragPixels ) {
    return scrollBarPixels - 2 * buttonLength - dragPixels;
  }
}