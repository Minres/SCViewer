/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.internal;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.ui.UiDispatch;
import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.IToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.IToolTipHelpTextProvider;

/** Owns the hover popup and its UI-thread listeners and dismissal timer. */
class ToolTipHandler implements AutoCloseable {
    private static final int HOVER_YOFFSET = 1;
    private static final int EXIT_CHECK_INTERVAL = 100;
    private static final int[] SOURCE_EVENTS = {
            SWT.Dispose, SWT.KeyDown, SWT.MouseHover, SWT.MouseDown, SWT.MouseMove, SWT.MouseExit
    };

    private final Display display;
    private final Shell parentShell;
    private final Supplier<Point> cursorLocation;
    private final Runnable exitCheck = this::checkPointer;
    private final Listener listener = this::handleEvent;
    private final HelpListener helpListener = event -> showHelp();
    private final Listener parentDeactivated;
    private final Listener parentDisposed = event -> close();
    private Shell shell;
    private Control control;
    private Point tipPosition;
    private boolean closed;

    public ToolTipHandler(Shell parent) {
        this(parent, parent.getDisplay()::getCursorLocation);
    }

    ToolTipHandler(Shell parent, Supplier<Point> cursorLocation) {
        display = parent.getDisplay();
        parentShell = parent;
        this.cursorLocation = cursorLocation;
        parentDeactivated = event -> UiDispatch.async(display, () -> {
            if (!closed && shell != null && display.getActiveShell() != shell) hide();
        });
        parent.addListener(SWT.Deactivate, parentDeactivated);
        parent.addListener(SWT.Dispose, parentDisposed);
    }

    public void activateHoverHelp(Control control) {
        if (closed) return;
        if (this.control != null) throw new IllegalStateException("Hover help already has an owner");
        this.control = control;
        for (int type : SOURCE_EVENTS) control.addListener(type, listener);
        control.addHelpListener(helpListener);
    }

    private void handleEvent(Event event) {
        if (closed) return;
        switch (event.type) {
        case SWT.KeyDown:
            if (shell != null && !shell.isDisposed() && event.keyCode == SWT.F2)
                shell.setFocus();
            break;
        case SWT.Dispose:
            close();
            break;
        case SWT.MouseDown:
        case SWT.MouseMove:
            hide();
            break;
        case SWT.MouseExit:
            // A canvas exit can mean entering the popup; defer until GTK has
            // updated the pointer location and compare display coordinates.
            UiDispatch.async(display, this::checkPointer);
            break;
        case SWT.MouseHover:
            showHover(event);
            break;
        default:
            break;
        }
    }

    private void showHover(Event event) {
        if (control == null || control.isDisposed() || parentShell.isDisposed()) return;
        Object value = control.getData(Constants.CONTENT_PROVIDER_TAG);
        if (!(value instanceof IToolTipContentProvider provider)) return;
        hide();
        Point point = new Point(event.x, event.y);
        tipPosition = control.toDisplay(point);
        Shell popup = new Shell(parentShell, SWT.NO_FOCUS | SWT.TOOL);
        shell = popup;
        popup.addDisposeListener(disposedEvent -> {
            if (shell == popup) {
                shell = null;
                tipPosition = null;
                if (!display.isDisposed()) display.timerExec(-1, exitCheck);
            }
        });
        popup.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        GridLayout layout = new GridLayout(1, true);
        layout.verticalSpacing = layout.horizontalSpacing = layout.marginWidth = layout.marginHeight = 0;
        popup.setLayout(new FillLayout());
        ScrolledComposite scroll = new ScrolledComposite(popup, SWT.H_SCROLL | SWT.V_SCROLL);
        Composite content = new Composite(scroll, SWT.NONE);
        content.setLayout(layout);
        scroll.setContent(content);
        content.addListener(SWT.Resize, resized -> resizeHover(popup, scroll, content));
        try {
            if (!provider.createContent(content, point)) { hide(); return; }
            content.pack();
            resizeHover(popup, scroll, content);
            watchPopupExit(popup, popup);
            popup.setVisible(true);
            display.timerExec(EXIT_CHECK_INTERVAL, exitCheck);
        } catch (RuntimeException | Error e) { hide(); throw e; }
    }

    private void resizeHover(Shell popup, ScrolledComposite scroll, Composite content) {
        Rectangle area = parentShell.getMonitor().getClientArea();
        Point size = content.getSize();
        Rectangle viewport = scroll.computeTrim(0, 0, size.x, size.y);
        Rectangle bounds = popup.computeTrim(0, 0, viewport.width, viewport.height);
        popup.setSize(Math.min(bounds.width, area.width), Math.min(bounds.height, area.height));
        popup.layout(true, true);
        setHoverLocation(popup, tipPosition);
    }

    private void watchPopupExit(Control child, Shell popup) {
        child.addListener(SWT.MouseExit, event -> UiDispatch.async(display, () -> {
            // Child-to-child transitions are not exits from the hover window.
            if (!closed && shell == popup && !popup.isDisposed()
                    && !popup.getBounds().contains(cursorLocation.get())) hide();
        }));
        if (child instanceof Composite composite)
            for (Control nested : composite.getChildren()) watchPopupExit(nested, popup);
    }

    private void checkPointer() {
        if (closed || shell == null || shell.isDisposed()) return;
        if (control == null || control.isDisposed() || !control.isVisible() || !shell.isVisible()) {
            hide();
            return;
        }
        Point pointer = cursorLocation.get();
        Point size = control.getSize();
        Rectangle owner = control instanceof Composite composite ? composite.getClientArea()
                : new Rectangle(0, 0, size.x, size.y);
        Point ownerPosition = control.toDisplay(owner.x, owner.y);
        owner.x = ownerPosition.x;
        owner.y = ownerPosition.y;
        if (!owner.contains(pointer) && !shell.getBounds().contains(pointer)) { hide(); return; }
        // MouseExit is not guaranteed by SWT when crossing native windows quickly.
        // Poll only while a popup exists; no mouse capture or global filter is needed.
        display.timerExec(EXIT_CHECK_INTERVAL, exitCheck);
    }

    private void hide() {
        if (!display.isDisposed()) display.timerExec(-1, exitCheck);
        Shell old = shell;
        shell = null;
        tipPosition = null;
        if (old != null && !old.isDisposed()) old.dispose();
    }

    private void showHelp() {
        if (closed || control == null || control.isDisposed() || shell == null || shell.isDisposed()) return;
        Object provider = control.getData(Constants.HELP_PROVIDER_TAG);
        if (!(provider instanceof IToolTipHelpTextProvider help)) return;
        String text = help.getHelpText(control);
        if (text == null) return;
        Point position = tipPosition;
        hide();
        Shell helpShell = new Shell(parentShell, SWT.SHELL_TRIM);
        helpShell.setLayout(new FillLayout());
        new Label(helpShell, SWT.NONE).setText(text);
        helpShell.pack();
        setHoverLocation(helpShell, position);
        helpShell.open();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        hide();
        if (!parentShell.isDisposed()) {
            parentShell.removeListener(SWT.Deactivate, parentDeactivated);
            parentShell.removeListener(SWT.Dispose, parentDisposed);
        }
        if (control != null && !control.isDisposed()) {
            for (int type : SOURCE_EVENTS) control.removeListener(type, listener);
            control.removeHelpListener(helpListener);
        }
        control = null;
    }

    private void setHoverLocation(Shell popup, Point position) {
        Rectangle displayBounds = parentShell.getMonitor().getClientArea();
        Rectangle bounds = popup.getBounds();
        bounds.x = Math.max(displayBounds.x, Math.min(position.x, displayBounds.x + displayBounds.width - bounds.width));
        bounds.y = Math.max(displayBounds.y, Math.min(position.y + HOVER_YOFFSET,
                displayBounds.y + displayBounds.height - bounds.height));
        popup.setBounds(bounds);
    }
}
