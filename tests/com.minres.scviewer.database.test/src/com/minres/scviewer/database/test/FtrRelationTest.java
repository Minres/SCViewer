package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.lang.reflect.Constructor;
import org.junit.Test;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxRelation;

public class FtrRelationTest {
    private static class Loader extends FtrDbLoader {
        ITx available;
        @Override public synchronized ITx getTransaction(long id) { return available; }
    }

    private ITx proxy(Loader loader) throws Exception {
        Class<?> raw = Class.forName("com.minres.scviewer.database.ftr.FtrRelation");
        Constructor<?> rawConstructor = raw.getDeclaredConstructor(RelationType.class, long.class,
                long.class, long.class, long.class);
        rawConstructor.setAccessible(true);
        Object relation = rawConstructor.newInstance(RelationTypeFactory.create("test"), 1L, 1L, 0L, 0L);
        Class<?> wrapper = Class.forName("com.minres.scviewer.database.ftr.TxRelation");
        Constructor<?> constructor = wrapper.getDeclaredConstructor(FtrDbLoader.class, raw);
        constructor.setAccessible(true);
        return ((ITxRelation) constructor.newInstance(loader, relation)).getSource();
    }

    @Test
    public void resolvesFromTimeGetterAndKeepsDuration() throws Exception {
        Loader loader = new Loader();
        ITx proxy = proxy(loader);
        TxTestSupport.Tx real = new TxTestSupport.Tx(1, 10, 50);
        real.streamRow = 2;
        real.generatorRow = 1;
        loader.available = real;
        assertEquals(50, proxy.getEndTime());
        assertEquals(2, proxy.getConcurrencyIndxInStream());
        assertEquals(1, proxy.getConcurrencyIdxInGen());
        assertEquals(10, proxy.getBeginTime());
        assertSame(real.getAttributes(), proxy.getAttributes());
        assertEquals(0, proxy.compareTo(proxy));
        assertEquals(0, proxy.compareTo(real));
        assertEquals(0, real.compareTo(proxy));
        TxTestSupport.Tx later = new TxTestSupport.Tx(2, 20, 60);
        assertEquals(-Integer.signum(later.compareTo(proxy)), Integer.signum(proxy.compareTo(later)));
    }

    @Test
    public void resolvesFromConcurrencyGetter() throws Exception {
        Loader loader = new Loader();
        ITx proxy = proxy(loader);
        TxTestSupport.Tx real = new TxTestSupport.Tx(1, 10, 50);
        real.streamRow = 3;
        real.generatorRow = 1;
        loader.available = real;
        assertEquals(1, proxy.getConcurrencyIdxInGen());
        assertEquals(3, proxy.getConcurrencyIndxInStream());
    }

    @Test
    public void missingEndpointFailsExplicitly() throws Exception {
        ITx proxy = proxy(new Loader());
        try { proxy.getBeginTime(); fail("Expected missing endpoint"); }
        catch (IllegalStateException e) { assertTrue(e.getMessage().contains("Missing related transaction")); }
    }
}
