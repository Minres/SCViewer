package com.minres.scviewer.database.ui.swt.sb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

// TODO [fappel]: This is a workaround for a problem described here:
// http://stackoverflow.com/questions/3908290/mousedown-events-are-not-delivered-until-mouseup-when-a-drag-source-is-present
// This seems to be related to https://bugs.eclipse.org/bugs/show_bug.cgi?id=328396
// which is resolved. As it did not work on my setup I adapted the workaround of the last
// stackoverflow answer.

public class DragDetector {

  int lastMouseX;
  int lastMouseY;
  boolean dragEventGenerated;

  private final Control control;
  private final int sensibility;

  public DragDetector( Control control, int sensibility ) {
    this.control = control;
    this.sensibility = sensibility;
    this.control.setDragDetect( false );
  }

  public void mouseMove( MouseEvent e ) {
    if( ( e.stateMask & SWT.BUTTON1 ) > 0 ) {
      int deltaX = lastMouseX - e.x;
      int deltaY = lastMouseY - e.y;
      int dragDistance = deltaX * deltaX + deltaY * deltaY;
      if( !dragEventGenerated && dragDistance > sensibility ) {
        dragEventGenerated = true;
        Event event = createDragEvent( e );
        control.notifyListeners( SWT.DragDetect, event );
      }
      lastMouseX = e.x;
      lastMouseY = e.y;
    }
  }

  public void dragHandled() {
    dragEventGenerated = false;
  }

  private Event createDragEvent( MouseEvent e ) {
    Event event = new Event();
    event.type = SWT.DragDetect;
    event.display = control.getDisplay();
    event.widget = control;
    event.button = e.button;
    event.stateMask = e.stateMask;
    event.time = e.time;
    event.x = e.x;
    event.y = e.y;
    return event;
  }
}