package com.minres.scviewer.e4.application.parts.txTableTree;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.Test;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;

public class TxFilterTest {
    private ITx transaction(String name, Object value) {
        ITxAttribute attribute = (ITxAttribute) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITxAttribute.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getValue" -> value;
                    default -> null;
                });
        return (ITx) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ITx.class},
                (proxy, method, args) -> method.getName().equals("getAttributes") ? List.of(attribute) : null);
    }

    @Test
    public void switchingToStringPropertyRecompilesExistingSearchText() {
        TxFilter filter = new TxFilter();
        filter.setSearchProp("number", DataType.INTEGER);
        filter.setSearchValue("match");
        filter.setSearchProp("text", DataType.STRING);
        assertFalse(filter.select(null, null, transaction("text", "different")));
        assertTrue(filter.select(null, null, transaction("text", "prefix match suffix")));
    }

    @Test
    public void backgroundFilterSnapshotIsUnaffectedBySubsequentUiEdits() {
        TxFilter filter = new TxFilter();
        filter.setSearchProp("text", DataType.STRING);
        filter.setSearchValue("old");
        java.util.function.Predicate<ITx> snapshot = filter.snapshot();
        filter.setSearchValue("new");
        assertTrue(snapshot.test(transaction("text", "old")));
        assertFalse(snapshot.test(transaction("text", "new")));
    }

    @Test
    public void booleanValuesCanBeMatchedWithoutAStringCast() {
        TxFilter filter = new TxFilter();
        filter.setSearchProp("flag", DataType.BOOLEAN);
        filter.setSearchValue("TRUE");
        assertTrue(filter.select(null, null, transaction("flag", true)));
        assertFalse(filter.select(null, null, transaction("flag", false)));
    }
}
