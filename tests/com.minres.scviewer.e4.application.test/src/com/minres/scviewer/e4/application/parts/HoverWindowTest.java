/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.IToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.IToolTipHelpTextProvider;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;

public class HoverWindowTest {
    private Display display;
    private Shell owner;
    private Canvas canvas;
    private Object handler;
    private Field popupField;
    private final AtomicReference<Point> pointer = new AtomicReference<>();
    private Label first, second;

    @Before public void createControls() throws Exception {
        assumeTrue("GTK integration tests require DISPLAY", !"gtk".equals(SWT.getPlatform())
                || System.getenv("DISPLAY") != null);
        display = new Display();
        owner = new Shell(display);
        owner.setBounds(100, 100, 600, 400);
        owner.setLayout(new FillLayout());
        canvas = new Canvas(owner, SWT.NONE);
        owner.open();
        drain();
        pointer.set(canvas.toDisplay(10, 10));
        Class<?> type = WaveformViewFactory.class.getClassLoader().loadClass(
                "com.minres.scviewer.database.ui.swt.internal.ToolTipHandler");
        var constructor = type.getDeclaredConstructor(Shell.class, Supplier.class);
        constructor.setAccessible(true);
        handler = constructor.newInstance(owner, (Supplier<Point>) pointer::get);
        var activate = type.getDeclaredMethod("activateHoverHelp", org.eclipse.swt.widgets.Control.class);
        activate.setAccessible(true);
        activate.invoke(handler, canvas);
        popupField = type.getDeclaredField("shell");
        popupField.setAccessible(true);
        canvas.setData(Constants.CONTENT_PROVIDER_TAG, (IToolTipContentProvider) (parent, point) -> {
            Composite content = new Composite(parent, SWT.NONE);
            content.setLayout(new FillLayout());
            first = new Label(content, SWT.NONE); first.setText("First label");
            second = new Label(content, SWT.NONE); second.setText("Second label");
            content.pack();
            return true;
        });
    }

    @After public void disposeControls() throws Exception {
        if (display == null) return;
        try {
            if (handler != null) {
                var close = handler.getClass().getDeclaredMethod("close");
                close.setAccessible(true); close.invoke(handler);
            }
            if (owner != null && !owner.isDisposed()) owner.dispose();
            drain();
        } finally { display.dispose(); }
    }

    private void drain() {
        int count = 0;
        while (display.readAndDispatch()) assertTrue("UI event loop did not settle", ++count < 2000);
    }

    private Shell popup() throws Exception { return (Shell) popupField.get(handler); }

    private Shell hover() throws Exception {
        pointer.set(canvas.toDisplay(10, 10));
        Event event = new Event(); event.x = 10; event.y = 10;
        canvas.notifyListeners(SWT.MouseHover, event);
        Shell popup = popup();
        assertNotNull(popup);
        assertTrue(popup.isVisible());
        return popup;
    }

    private void assertBoundedAndScrollable(Shell popup) {
        Rectangle area = owner.getMonitor().getClientArea(), bounds = popup.getBounds();
        assertTrue(bounds.x >= area.x && bounds.y >= area.y);
        assertTrue(bounds.x + bounds.width <= area.x + area.width);
        assertTrue(bounds.y + bounds.height <= area.y + area.height);
        ScrolledComposite scroll = (ScrolledComposite) popup.getChildren()[0];
        assertTrue(scroll.getHorizontalBar().getVisible());
        assertTrue(scroll.getVerticalBar().getVisible());
        scroll.setOrigin(area.width, area.height);
        assertTrue("Wide content must be reachable", scroll.getOrigin().x > 0);
        assertTrue("Tall content must be reachable", scroll.getOrigin().y > 0);
    }

    @Test public void oversizedContentStaysOnScreenAndScrollsInBothDirections() throws Exception {
        Rectangle area = owner.getMonitor().getClientArea();
        canvas.setData(Constants.CONTENT_PROVIDER_TAG, (IToolTipContentProvider) (parent, point) -> {
            Label label = new Label(parent, SWT.NONE);
            label.setText("Oversized hover content");
            label.setLayoutData(new GridData(area.width * 2, area.height * 2));
            return true;
        });
        assertBoundedAndScrollable(hover());
    }

    @Test public void contentLoadedAfterOpeningAlsoStaysBoundedAndScrollable() throws Exception {
        Shell popup = hover();
        ScrolledComposite scroll = (ScrolledComposite) popup.getChildren()[0];
        Composite content = (Composite) scroll.getContent();
        assertFalse(scroll.getHorizontalBar().getVisible());
        assertFalse(scroll.getVerticalBar().getVisible());
        Rectangle area = owner.getMonitor().getClientArea();
        first.getParent().setLayoutData(new GridData(area.width * 2, area.height * 2));
        // The transaction hover packs its content after asynchronous attributes arrive.
        content.pack();
        assertBoundedAndScrollable(popup);
    }

    @Test public void sourceMovementAndExitingTheCanvasDismissHover() throws Exception {
        Shell moving = hover();
        canvas.notifyListeners(SWT.MouseMove, new Event());
        assertTrue(moving.isDisposed());
        Shell exiting = hover();
        pointer.set(new Point(-10000, -10000));
        canvas.notifyListeners(SWT.MouseExit, new Event());
        drain();
        assertTrue(exiting.isDisposed());
        assertNull(popup());
    }

    @Test public void enteringPopupAndCrossingChildrenKeepsItOpenButLeavingClosesIt() throws Exception {
        Shell popup = hover();
        pointer.set(second.toDisplay(1, 1));
        assertTrue(popup.getBounds().contains(pointer.get()));
        canvas.notifyListeners(SWT.MouseExit, new Event());
        first.notifyListeners(SWT.MouseExit, new Event());
        drain();
        assertFalse(popup.isDisposed());
        // Leaving a child for the original canvas is also leaving the hover window.
        pointer.set(canvas.toDisplay(canvas.getSize().x - 5, canvas.getSize().y - 5));
        assertFalse(popup.getBounds().contains(pointer.get()));
        second.notifyListeners(SWT.MouseExit, new Event());
        drain();
        assertTrue(popup.isDisposed());
    }

    @Test public void pointerCheckDismissesHoverWhenNativeExitEventIsMissing() throws Exception {
        Shell popup = hover();
        pointer.set(new Point(-10000, -10000));
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!popup.isDisposed()) {
            drain();
            assertTrue("Hover did not disappear outside the window", System.nanoTime() < deadline);
            Thread.sleep(5);
        }
        assertNull(popup());
    }

    @Test public void ownerDisposalCancelsChecksAndQueuedExitCallbacks() throws Exception {
        Shell popup = hover();
        canvas.notifyListeners(SWT.MouseExit, new Event());
        canvas.dispose();
        drain();
        assertTrue(popup.isDisposed());
        assertNull(popup());
        assertFalse(owner.isListening(SWT.Deactivate));
    }

    @Test public void emptyContentDisposesPopupAndHelpUsesTheActiveHoverShell() throws Exception {
        canvas.setData(Constants.HELP_PROVIDER_TAG, (IToolTipHelpTextProvider) widget -> "Hover help");
        Shell popup = hover();
        canvas.notifyListeners(SWT.Help, new Event());
        assertTrue(popup.isDisposed());
        assertNull(popup());
        assertTrue(java.util.Arrays.stream(display.getShells()).anyMatch(shell -> shell != owner && !shell.isDisposed()));
        canvas.setData(Constants.CONTENT_PROVIDER_TAG, (IToolTipContentProvider) (parent, point) -> false);
        Event event = new Event(); event.x = 10; event.y = 10;
        canvas.notifyListeners(SWT.MouseHover, event);
        assertNull(popup());
    }
}
