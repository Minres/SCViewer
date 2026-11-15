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
package com.minres.scviewer.e4.application.internal.util;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class FileMonitorTest {
    @Test
    public void collidingListenersAreIndependentAndCancelledCheckersStayInactive() throws Exception {
        FileMonitor monitor = new FileMonitor();
        Path file = Files.createTempFile("scviewer-watch", ".tmp");
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        IFileChangeListener a = listener(first);
        IFileChangeListener b = listener(second);
        try {
            List<java.io.File> files = new ArrayList<>(List.of(file.toFile()));
            IModificationChecker ca = monitor.addFileChangeListener(a, files, 60000);
            IModificationChecker cb = monitor.addFileChangeListener(b, files, 60000);
            files.clear();
            Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10000));
            ca.check(); cb.check();
            assertEquals(1, first.get()); assertEquals(1, second.get());
            monitor.removeFileChangeListener(a);
            Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10000));
            ca.check(); cb.check();
            assertEquals(1, first.get()); assertEquals(2, second.get());
            monitor.dispose();
			monitor.setEnabled(true);
			assertFalse(monitor.isEnabled());
            Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10000));
            cb.check();
            assertEquals(2, second.get());
        } finally { monitor.dispose(); Files.deleteIfExists(file); }
    }

    private IFileChangeListener listener(AtomicInteger count) {
        return new IFileChangeListener() {
            @Override public void fileChanged(List<java.io.File> files) { count.incrementAndGet(); }
            @Override public int hashCode() { return 1; }
        };
    }
}
