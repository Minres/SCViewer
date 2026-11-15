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
package com.minres.scviewer.database.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.text.TextDbLoader;
import com.minres.scviewer.database.sqlite.SQLiteDbLoader;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITransactionProvider;

public class TransactionIndexTest {
    @Test
    public void transactionListsAreCachedAndLookupsRespectGeneratorMembership() throws Exception {
        IWaveformDbLoader[] loaders = {new FtrDbLoader(), new TextDbLoader(), new SQLiteDbLoader()};
        String[] inputs = {"my_db.ftr", "my_db.txlog", "my_sqldb.txdb"};
        for (int i = 0; i < loaders.length; i++) {
            IWaveformDbLoader loader = loaders[i];
            try {
                loader.load(TxTestSupport.input(inputs[i]));
                for (IWaveform wave : loader.getAllWaves()) {
                    if (!wave.getType().isTransaction()) continue;
                    assertTrue(wave instanceof ITransactionProvider);
                    check((ITransactionProvider) wave);
                    for (com.minres.scviewer.database.IHierNode child : wave.getChildNodes()) {
                        if (!(child instanceof ITransactionProvider generator)) continue;
                        check(generator);
                        for (ITx transaction : ((ITransactionProvider) wave).getTransactionList())
                            if (transaction.getGenerator() != child) assertNull(generator.getTransaction(transaction.getId()));
                    }
                }
            } finally { loader.dispose(); }
        }
    }

    private void check(ITransactionProvider provider) {
        List<ITx> transactions = provider.getTransactionList();
        assertSame(transactions, provider.getTransactionList());
        for (int i = 0; i < transactions.size(); i++) {
            ITx transaction = transactions.get(i);
            assertSame(transaction, provider.getTransaction(transaction.getId()));
            if (i > 0) assertTrue(transactions.get(i - 1).compareTo(transaction) <= 0);
        }
        assertNull(provider.getTransaction(Long.MAX_VALUE));
        try { transactions.clear(); fail("Index list must be immutable"); }
        catch (UnsupportedOperationException expected) {}
    }
}
