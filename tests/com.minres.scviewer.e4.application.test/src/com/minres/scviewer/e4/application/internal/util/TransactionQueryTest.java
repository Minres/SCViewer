package com.minres.scviewer.e4.application.internal.util;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Test;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITransactionProvider;

public class TransactionQueryTest {
    private static class Waveform extends EmptyWaveform implements ITransactionProvider {
        List<ITx> transactions;
        int requests;
        @Override public List<ITx> getTransactionList() { requests++; return transactions; }
        @Override public ITx getTransaction(long id) { return transactions.stream().filter(tx -> tx.getId() == id).findFirst().orElse(null); }
        @Override public IEventList getEvents() { throw new AssertionError("Indexed access must not enumerate waveform events"); }
    }

    private ITx transaction(long id, AtomicInteger attributeReads) {
        ITxAttribute attribute = (ITxAttribute) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITxAttribute.class},
                (p, m, a) -> switch (m.getName()) { case "getName" -> "field"; case "getDataType" -> DataType.STRING; default -> "value"; });
        return (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class}, (p, m, a) -> {
            if (m.getName().equals("getAttributes")) { attributeReads.incrementAndGet(); return List.of(attribute); }
            return id;
        });
    }

    @Test
    public void discoveryIncludesLatePropertiesAndCachesTheCompleteSchema() {
        AtomicInteger reads = new AtomicInteger();
        Waveform wave = new Waveform();
        ITxAttribute lateAttribute = (ITxAttribute) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ITxAttribute.class}, (p, m, a) -> switch (m.getName()) {
                    case "getName" -> "late";
                    case "getDataType" -> DataType.STRING;
                    default -> "later value";
                });
        ITx late = (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class},
                (p, m, a) -> {
                    if (m.getName().equals("getAttributes")) { reads.incrementAndGet(); return List.of(lateAttribute); }
                    return 3L;
                });
        wave.transactions = List.of(transaction(1, reads), transaction(2, reads), late);
        TransactionQuery query = new TransactionQuery(wave);
        assertEquals(List.of(new TransactionQuery.Attribute("field", DataType.STRING),
                new TransactionQuery.Attribute("late", DataType.STRING)), query.discoverAttributes(() -> false));
        assertEquals(3, reads.get());
        query.discoverAttributes(() -> false);
        assertEquals(3, reads.get());
    }

    @Test
    public void headerQueriesAndIndexedRestorationNeverReadAttributesOrEvents() {
        AtomicInteger reads = new AtomicInteger();
        Waveform wave = new Waveform();
        ITx first = transaction(1, reads), second = transaction(2, reads);
        wave.transactions = List.of(first, second);
        TransactionQuery query = new TransactionQuery(wave);
        assertSame(wave.transactions, query.transactions(() -> false));
        assertSame(wave.transactions, query.transactions(() -> false));
        assertEquals(1, wave.requests);
        assertSame(second, ViewerSettings.findTransaction(wave, 2));
        assertNull(ViewerSettings.findTransaction(wave, 3));
        assertEquals(0, reads.get());
        assertEquals(1, query.filter(tx -> tx.getId() == 2, () -> false).size());
        assertEquals(0, reads.get());
        assertEquals(List.of(new TransactionQuery.Attribute("field", DataType.STRING)), query.discoverAttributes(() -> false));
        assertEquals(2, reads.get());
        query.discoverAttributes(() -> false);
        assertEquals("Property discovery is cached for this track", 2, reads.get());
    }

    @Test
    public void cancelledFallbackBuildCanBeRetriedForTheSameTrack() {
        AtomicInteger reads = new AtomicInteger();
        ITx tx = transaction(1, reads);
        com.minres.scviewer.database.tx.ITxEvent event = (com.minres.scviewer.database.tx.ITxEvent) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{com.minres.scviewer.database.tx.ITxEvent.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getTransaction" -> tx;
                    case "getKind" -> com.minres.scviewer.database.EventKind.SINGLE;
                    default -> 1L;
                });
        com.minres.scviewer.database.EventList.Builder builder = new com.minres.scviewer.database.EventList.Builder();
        builder.add(1, event);
        IEventList events = builder.build();
        EmptyWaveform waveform = new EmptyWaveform() {
            @Override public IEventList getEvents() { return events; }
        };
        TransactionQuery query = new TransactionQuery(waveform);
        AtomicInteger checks = new AtomicInteger();
        try { query.transactions(() -> checks.incrementAndGet() >= 2); fail("Expected cancellation"); }
        catch (OperationCanceledException expected) {}
        assertEquals(1, query.transactions(() -> false).size());
        assertEquals(0, reads.get());
    }

    @Test
    public void cancelledQueriesStopBeforeLoadingAttributeValues() {
        Waveform wave = new Waveform();
        AtomicInteger reads = new AtomicInteger();
        wave.transactions = List.of(transaction(1, reads));
        TransactionQuery query = new TransactionQuery(wave);
        try { query.discoverAttributes(() -> true); fail("Cancelled query ran"); }
        catch (OperationCanceledException expected) {}
        assertEquals(0, reads.get());
        assertEquals(0, wave.requests);
    }
}
