package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Label;

class ImageUpdate {

  private final ImageDrawer imageDrawer;
  private final Label control;

  ImageUpdate( Label control, int maxExpansion ) {
    this.imageDrawer = new ImageDrawer( maxExpansion );
    this.control = control;
    this.control.addListener( SWT.Dispose, evt -> imageDrawer.dispose() );
  }

  void setForeground( Color color ) {
    imageDrawer.setForeground( color );
  }

  Color getForeground() {
    return imageDrawer.getForeground();
  }

  void setBackground( Color color ) {
    imageDrawer.setBackground( color );
  }

  Color getBackground() {
    return imageDrawer.getBackground();
  }

  void update() {
    if( !control.isDisposed() ) {
      if( control.getImage() != null ) {
        control.getImage().dispose();
      }
      Point size = control.getSize();
      if( size.x > 0 && size.y > 0 ) {
        control.setImage( imageDrawer.draw( size.x, size.y ) );
      }
    }
  }
}