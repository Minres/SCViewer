/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.internal.status;

import static org.junit.Assert.*;
import org.junit.Test;

public class JobProgressTest {
    @Test public void cancellationAndProgressBelongToOneJob() {
        JobProgress first = new JobProgress(() -> {}), second = new JobProgress(() -> {});
        first.beginTask("first", 10); second.beginTask("second", 20);
        first.worked(3); second.worked(8); first.setCanceled(true); first.done();
        assertTrue(first.isCanceled()); assertFalse(second.isCanceled());
        assertTrue(first.snapshot().done()); assertFalse(second.snapshot().done());
        assertEquals("second", second.snapshot().name());
        assertEquals(8, second.snapshot().work(), 0);
        assertFalse(new JobProgress(() -> {}).isCanceled());
    }
}
