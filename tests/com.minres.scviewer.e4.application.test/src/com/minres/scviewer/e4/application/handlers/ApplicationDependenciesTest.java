/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.eclipse.osgi.util.ManifestElement;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

import com.minres.scviewer.e4.application.E4LifeCycle;

public class ApplicationDependenciesTest {
    @Test
    public void lifecycleEventTypesAreDeclaredByTheProductionBundle() throws Exception {
        var application = FrameworkUtil.getBundle(E4LifeCycle.class);
        var imports = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE,
                application.getHeaders().get(Constants.IMPORT_PACKAGE));
        // A fragment import also makes these classes visible during tests, but
        // the product does not contain the test fragment.
        assertTrue("The application must import Event Admin without the test fragment",
                Arrays.stream(imports).anyMatch(element -> element.getValue().equals("org.osgi.service.event")));
        assertNotNull(application.loadClass("org.osgi.service.event.Event"));
        assertNotNull(application.loadClass("org.osgi.service.event.EventHandler"));
        assertTrue(E4LifeCycle.class.getDeclaredMethods().length > 0);
    }
}
