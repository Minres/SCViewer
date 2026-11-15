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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import com.minres.scviewer.database.LazyValue;

public class LazyValueTest {
    @Test
    public void concurrentEmptyValueLoadsOnce() throws Exception {
        LazyValue<List<String>> lazy = new LazyValue<>();
        AtomicInteger calls = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<List<String>>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; ++i) tasks.add(() -> lazy.get(() -> {
                calls.incrementAndGet();
                return java.util.Collections.emptyList();
            }));
            for (Future<List<String>> result : executor.invokeAll(tasks)) assertTrue(result.get().isEmpty());
            assertEquals(1, calls.get());
        } finally { executor.shutdownNow(); }
    }

    @Test
    public void preservesFailureWithoutRetry() {
        LazyValue<String> lazy = new LazyValue<>();
        IllegalStateException failure = new IllegalStateException("Invalid input");
        for (int i = 0; i < 2; ++i) {
            try { lazy.get(() -> { throw failure; }); fail("Expected failure"); }
            catch (IllegalStateException e) { assertSame(failure, e); }
        }
    }
}
