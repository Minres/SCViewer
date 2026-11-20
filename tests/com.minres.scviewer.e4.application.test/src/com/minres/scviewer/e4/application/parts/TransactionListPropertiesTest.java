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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITransactionProvider;
import com.minres.scviewer.database.ui.TrackEntry;

public class TransactionListPropertiesTest {
    private static class Stream extends EmptyWaveform implements ITransactionProvider {
        List<ITx> transactions;
        @Override public WaveformType getType() { return WaveformType.STREAM; }
        @Override public List<ITx> getTransactionList() { return transactions; }
        @Override public ITx getTransaction(long id) { return transactions.stream().filter(tx -> tx.getId() == id).findFirst().orElse(null); }
        @Override public IEventList getEvents() { throw new AssertionError("Property discovery must use indexed headers"); }
    }

    private Display display;
    private Shell shell;
    private WaveformViewer viewer;
    private TransactionList list;
    private TransactionListView listView;
    private Combo search;
    private Combo shown;
    private Table table;

    @Before
    public void createControls() {
        assumeTrue("GTK integration tests require DISPLAY", !"gtk".equals(SWT.getPlatform()) || System.getenv("DISPLAY") != null);
        display = new Display();
        shell = new Shell(display);
        shell.setSize(1000, 500);
        viewer = new WaveformViewer() {
            @Override public String getScaledTime(Long time) { return time.toString(); }
        };
        listView = new TransactionListView();
        listView.createComposite(shell, viewer);
        list = listView.getControl();
        var combos = Arrays.stream(list.getChildren()).filter(Combo.class::isInstance).map(Combo.class::cast).toList();
        search = combos.get(0); shown = combos.get(1);
        table = Arrays.stream(list.getChildren()).filter(Table.class::isInstance).map(Table.class::cast).findFirst().orElseThrow();
        assertFalse(Arrays.stream(list.getChildren()).anyMatch(Button.class::isInstance));
        shell.layout(true, true);
    }

    @After
    public void disposeControls() {
        if (display == null) return;
        try {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (viewer != null) viewer.closeDatabase();
            while (display.readAndDispatch()) {}
        } finally { display.dispose(); }
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!condition.getAsBoolean()) {
            while (display.readAndDispatch()) {}
            if (System.nanoTime() > deadline) fail("Timed out waiting for transaction properties");
            Thread.sleep(5);
        }
    }

    private ITxAttribute attribute(String name, String value) {
        return (ITxAttribute) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITxAttribute.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getName" -> name;
                    case "getDataType" -> DataType.STRING;
                    case "getValue" -> value;
                    default -> null;
                });
    }

    private ITx transaction(long id, ITxAttribute... attributes) {
        return (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getId" -> id;
                    case "getBeginTime" -> 0L;
                    case "getEndTime" -> 1L;
                    case "getAttributes" -> {
                        assertNull("A label callback read attributes on SWT", Display.getCurrent());
                        yield List.of(attributes);
                    }
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == a[0];
                    case "toString" -> "tx#" + id;
                    default -> null;
                });
    }

    private void input(ITx... transactions) {
        Stream stream = new Stream();
        stream.transactions = List.of(transactions);
        listView.setSelection(new StructuredSelection(new TrackEntry(stream, null)));
    }

    @Test
    public void automaticDiscoveryPreservesPropertyChosenByDetailsSearch() throws Exception {
        List<ITx> transactions = new ArrayList<>();
        for (int i = 0; i < 32; i++) transactions.add(transaction(i, attribute("alpha", "first")));
        transactions.add(transaction(32, attribute("late", "last")));
        input(transactions.toArray(new ITx[0]));
        list.setSearchProps("late", DataType.STRING, "last");
        await(() -> shown.getItemCount() == 2 && table.getItemCount() == 1);
        assertEquals("late [STRING]", search.getText());
        assertEquals("late [STRING]", shown.getText());
        await(() -> "last".equals(table.getItem(0).getText(2)));
        assertEquals("last", table.getItem(0).getText(2));
    }

    @Test
    public void selectingTrackAutomaticallyShowsPropertyNamesAndValues() throws Exception {
        input(transaction(1, attribute("address", "value")));
        await(() -> shown.getSelectionIndex() >= 0 && table.getItemCount() == 1);
        assertEquals("address [STRING]", shown.getText());
        assertEquals("address [STRING]", search.getText());
        await(() -> "value".equals(table.getItem(0).getText(2)));
        assertEquals("value", table.getItem(0).getText(2));
    }

    @Test
    public void automaticDiscoveryFindsPropertiesBeyondTheFirst32Transactions() throws Exception {
        List<ITx> transactions = new ArrayList<>();
        for (int i = 0; i < 32; i++) transactions.add(transaction(i, attribute("alpha", "first"), attribute("beta", "second")));
        transactions.add(transaction(32, attribute("late", "last")));
        input(transactions.toArray(new ITx[0]));
        await(() -> shown.getItemCount() == 3 && table.getItemCount() == 33);
        shown.select(1); shown.notifyListeners(SWT.Selection, new Event());
        await(() -> "second".equals(table.getItem(0).getText(2)));
        assertEquals("second", table.getItem(0).getText(2));
        assertTrue(Arrays.asList(shown.getItems()).contains("late [STRING]"));
        list.setVisible(false);
        list.setVisible(true);
        await(() -> table.getItemCount() == 33);
        assertEquals("beta [STRING]", shown.getText());
    }

    @Test
    public void remembersDisplayPropertyAcrossStreamsAndMissingProperties() throws Exception {
        input(transaction(1, attribute("alpha", "first"), attribute("beta", "chosen")));
        await(() -> shown.getItemCount() == 2 && table.getItemCount() == 1);
        shown.select(1); shown.notifyListeners(SWT.Selection, new Event());
        await(() -> "chosen".equals(table.getItem(0).getText(2)));
        assertEquals("chosen", table.getItem(0).getText(2));

        // The preferred property is no longer the second entry in the new stream.
        input(transaction(2, attribute("aardvark", "a"), attribute("alpha", "b"), attribute("beta", "restored")));
        await(() -> shown.getItemCount() == 3 && table.getItemCount() == 1);
        assertEquals("beta [STRING]", shown.getText());
        await(() -> "restored".equals(table.getItem(0).getText(2)));
        assertEquals("restored", table.getItem(0).getText(2));

        // A temporary fallback and a separator must not erase the user's preference.
        input(transaction(3, attribute("other", "fallback")));
        await(() -> shown.getText().equals("other [STRING]") && table.getItemCount() == 1);
        await(() -> "fallback".equals(table.getItem(0).getText(2)));
        assertEquals("fallback", table.getItem(0).getText(2));
        listView.setSelection(new StructuredSelection(new TrackEntry(new EmptyWaveform(), null)));
        input(transaction(4, attribute("alpha", "default"), attribute("beta", "remembered")));
        await(() -> shown.getItemCount() == 2 && table.getItemCount() == 1);
        assertEquals("beta [STRING]", shown.getText());
        await(() -> "remembered".equals(table.getItem(0).getText(2)));
        assertEquals("remembered", table.getItem(0).getText(2));

        // A subsequent explicit choice replaces the remembered preference.
        shown.select(0); shown.notifyListeners(SWT.Selection, new Event());
        input(transaction(5, attribute("aardvark", "default"), attribute("alpha", "new preference"), attribute("beta", "old")));
        await(() -> shown.getItemCount() == 3 && table.getItemCount() == 1);
        assertEquals("alpha [STRING]", shown.getText());
        await(() -> "new preference".equals(table.getItem(0).getText(2)));
        assertEquals("new preference", table.getItem(0).getText(2));
    }

    @Test
    public void hiddenViewDefersLoadingUntilShown() throws Exception {
        list.setVisible(false);
        input(transaction(1, attribute("address", "value")));
        // Pump queued work: selecting a track while hidden must not start discovery.
        for (int i = 0; i < 10; i++) {
            while (display.readAndDispatch()) {}
            Thread.sleep(5);
        }
        assertEquals(0, shown.getItemCount());
        assertEquals(0, table.getItemCount());
        list.setVisible(true);
        await(() -> shown.getItemCount() == 1 && table.getItemCount() == 1);
        assertEquals("address [STRING]", shown.getText());
        await(() -> "value".equals(table.getItem(0).getText(2)));
        assertEquals("value", table.getItem(0).getText(2));
    }

    @Test
    public void switchingTracksReplacesPropertiesAndSeparatorClearsThem() throws Exception {
        input(transaction(1, attribute("old", "old value")));
        input(transaction(2, attribute("new", "new value")));
        await(() -> shown.getText().equals("new [STRING]") && table.getItemCount() == 1);
        await(() -> "new value".equals(table.getItem(0).getText(2)));
        assertEquals("new value", table.getItem(0).getText(2));
        listView.setSelection(new StructuredSelection(new TrackEntry(new EmptyWaveform(), null)));
        assertEquals(0, shown.getItemCount());
        assertEquals(0, search.getItemCount());
        assertEquals(0, table.getItemCount());
    }
}
