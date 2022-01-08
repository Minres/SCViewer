package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


class ClickControl extends ControlAdapter implements ViewComponent, MouseDownActionTimer.TimerAction, MouseListener, MouseTrackListener {

  private final MouseDownActionTimer mouseDownActionTimer;
  private final ClickAction clickAction;
  private final ButtonClick buttonClick;
  private final Label control;
  private final ImageUpdate imageUpdate;

  public interface ClickAction extends Runnable {
    void setCoordinates( int x, int y );
  }

  ClickControl( Composite parent, ClickAction clickAction, int maxExtension  ) {
    this.control = new Label( parent, SWT.NONE );
    this.imageUpdate = new ImageUpdate( control, maxExtension );
    this.buttonClick = new ButtonClick();
    this.mouseDownActionTimer = new MouseDownActionTimer( this, buttonClick, control.getDisplay() );
    this.clickAction = clickAction;
    this.control.addMouseTrackListener( this );
    this.control.addMouseListener( this );
    this.control.addControlListener( this );
  }

  @Override
  public void controlResized( ControlEvent event ) {
    imageUpdate.update();
  }

  @Override
  public Label getControl() {
    return control;
  }

  @Override
  public void mouseDown( MouseEvent event ) {
    buttonClick.arm( event );
    clickAction.setCoordinates( event.x, event.y );
    mouseDownActionTimer.activate();
  }

  @Override
  public void mouseUp( MouseEvent event ) {
    buttonClick.trigger( event, clickAction );
  }

  @Override
  public void run() {
    clickAction.run();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void mouseExit( MouseEvent event ) {
    buttonClick.disarm();
  }

  void setForeground( Color color ) {
    imageUpdate.setForeground( color );
  }

  Color getForeground() {
    return imageUpdate.getForeground();
  }

  void setBackground( Color color ) {
    imageUpdate.setBackground( color );
  }

  Color getBackground() {
    return imageUpdate.getBackground();
  }

  @Override
  public void mouseEnter( MouseEvent event ) {}

  @Override
  public void mouseHover( MouseEvent event ) {}

  @Override
  public void mouseDoubleClick( MouseEvent event ) {}
}
