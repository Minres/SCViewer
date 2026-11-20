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
