/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.ui.AsyncTransactionData;
import com.minres.scviewer.database.ui.UiDispatch;

public class TransactionHoverTest {
    private Display display;
    private WaveformViewer viewer;
    private Method snapshot, refresh;
    private final CountDownLatch loading = new CountDownLatch(1), release = new CountDownLatch(1);
    private final AtomicInteger reads = new AtomicInteger();
    private ITx transaction;

    @Before public void setup() throws Exception {
        assumeTrue("GTK integration tests require DISPLAY", !"gtk".equals(SWT.getPlatform())
                || System.getenv("DISPLAY") != null);
        display = new Display();
        viewer = new WaveformViewer();
        viewer.sync = new UISynchronize() {
            @Override public void asyncExec(Runnable runnable) { UiDispatch.async(display, runnable); }
            @Override public void syncExec(Runnable runnable) { display.syncExec(runnable); }
            @Override protected boolean isUIThread(Thread thread) { return thread == display.getThread(); }
            @Override protected void showBusyWhile(Runnable runnable) { runnable.run(); }
            @Override protected boolean dispatchEvents() { return display.readAndDispatch(); }
        };
        snapshot = WaveformViewer.class.getDeclaredMethod("transactionSnapshot", ITx.class);
        snapshot.setAccessible(true);
        refresh = WaveformViewer.class.getDeclaredMethod("refreshHoverWhenReady", Composite.class, ITx.class, Runnable.class);
        refresh.setAccessible(true);
        ITxAttribute attribute = (ITxAttribute) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITxAttribute.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "address";
                    case "getValue" -> "0x1234";
                    case "getDataType" -> DataType.STRING;
                    default -> null;
                });
        transaction = (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAttributes")) {
                        assertNull("Hover attributes must load off SWT", Display.getCurrent());
                        reads.incrementAndGet(); loading.countDown();
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                        return List.of(attribute);
                    }
                    if (method.getName().endsWith("Relations")) return List.of();
                    return null;
                });
    }

    @After public void cleanup() {
        release.countDown();
        if (display == null) return;
        viewer.closeDatabase();
        for (Shell shell : display.getShells()) if (!shell.isDisposed()) shell.dispose();
        while (display.readAndDispatch()) { }
        display.dispose();
    }

    private AsyncTransactionData.Snapshot data() {
        try { return (AsyncTransactionData.Snapshot) snapshot.invoke(viewer, transaction); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private Table show(Shell popup, AtomicInteger updates) throws Exception {
        Table table = new Table(popup, SWT.NONE);
        new TableColumn(table, SWT.NONE); new TableColumn(table, SWT.NONE);
        refresh.invoke(viewer, popup, transaction, (Runnable) () -> {
            assertSame(display, Display.getCurrent());
            updates.incrementAndGet();
            table.removeAll();
            for (ITxAttribute attribute : data().attributes())
                new TableItem(table, SWT.NONE).setText(new String[]{attribute.getName(), attribute.getValue().toString()});
        });
        return table;
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            while (display.readAndDispatch()) { }
            assertTrue("Attribute completion was not published", System.nanoTime() < deadline);
            Thread.sleep(5);
        }
    }

    @Test public void firstHoverPopulatesWhenTheBackgroundReadCompletes() throws Exception {
        AtomicInteger updates = new AtomicInteger();
        Table table = show(new Shell(display), updates);
        assertTrue(loading.await(5, TimeUnit.SECONDS));
        assertEquals(0, table.getItemCount());
        release.countDown();
        await(() -> table.getItemCount() == 1);
        assertEquals("address", table.getItem(0).getText(0));
        assertEquals("0x1234", table.getItem(0).getText(1));
        assertEquals(2, updates.get());
        assertEquals(1, reads.get());
    }

    @Test public void closingAnOldPopupDoesNotDetachItsReplacement() throws Exception {
        Shell old = new Shell(display);
        AtomicInteger oldUpdates = new AtomicInteger(), newUpdates = new AtomicInteger();
        show(old, oldUpdates);
        assertTrue(loading.await(5, TimeUnit.SECONDS));
        Table replacement = show(new Shell(display), newUpdates);
        old.dispose();
        release.countDown();
        await(() -> replacement.getItemCount() == 1);
        assertEquals(1, oldUpdates.get());
        assertEquals(2, newUpdates.get());
        assertEquals(1, reads.get());
    }

    @Test public void aClosedPopupIsNotRefreshedAfterLoading() throws Exception {
        Shell popup = new Shell(display);
        AtomicInteger updates = new AtomicInteger();
        show(popup, updates);
        assertTrue(loading.await(5, TimeUnit.SECONDS));
        popup.dispose(); release.countDown();
        var field = WaveformViewer.class.getDeclaredField("transactionSnapshots"); field.setAccessible(true);
        AsyncTransactionData cache = (AsyncTransactionData) field.get(viewer);
        await(() -> cache.isReady(transaction));
        assertEquals(1, updates.get());
    }
}
