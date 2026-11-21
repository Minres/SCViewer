/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.tx;

import java.util.List;

/** Optional indexed access scoped to one transaction stream or generator. */
public interface ITransactionProvider {
    /** Immutable list ordered by begin time and transaction ID; does not load attributes. */
    List<ITx> getTransactionList();

    /** Returns null when the ID does not belong to this stream or generator. */
    ITx getTransaction(long id);
}
