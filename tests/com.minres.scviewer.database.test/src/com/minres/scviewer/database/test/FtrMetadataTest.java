/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.lang.reflect.Method;
import org.junit.Test;
import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.tx.ITxAttributeType;

public class FtrMetadataTest {
    @Test
    public void sameNameKeepsTypeAndAssociation() throws Exception {
        FtrDbLoader loader = new FtrDbLoader();
        Method method = FtrDbLoader.class.getDeclaredMethod("getOrAddAttributeType", long.class, DataType.class, String.class);
        method.setAccessible(true);
        ITxAttributeType begin = (ITxAttributeType) method.invoke(loader, 7L, DataType.INTEGER, "value");
        ITxAttributeType end = (ITxAttributeType) method.invoke(loader, 9L, DataType.INTEGER, "value");
        ITxAttributeType string = (ITxAttributeType) method.invoke(loader, 7L, DataType.STRING, "value");
        assertEquals(AssociationType.BEGIN, begin.getType());
        assertEquals(AssociationType.END, end.getType());
        assertEquals(DataType.STRING, string.getDataType());
        assertNotSame(begin, end);
        assertNotSame(begin, string);
        assertSame(begin, method.invoke(loader, 7L, DataType.INTEGER, "value"));
    }

    @Test
    public void parsedRelationsAreRegistered() throws Exception {
        FtrDbLoader loader = new FtrDbLoader();
        try {
            loader.load(TxTestSupport.input("my_db.ftr"));
            assertFalse(loader.getAllRelationTypes().isEmpty());
            for (com.minres.scviewer.database.IWaveform wave : loader.getAllWaves())
                for (com.minres.scviewer.database.EventEntry entry : wave.getEvents())
                    for (com.minres.scviewer.database.IEvent e : entry.events) {
                        com.minres.scviewer.database.tx.ITx tx = ((com.minres.scviewer.database.tx.ITxEvent)e).getTransaction();
                        for (com.minres.scviewer.database.tx.ITxRelation relation : tx.getOutgoingRelations())
                            assertTrue(loader.getAllRelationTypes().contains(relation.getRelationType()));
                    }
        } finally { loader.dispose(); }
    }
}
