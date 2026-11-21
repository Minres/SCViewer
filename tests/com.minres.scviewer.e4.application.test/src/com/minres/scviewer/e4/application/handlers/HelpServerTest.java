/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import org.junit.Test;
import static org.junit.Assert.*;

@SuppressWarnings("restriction")
public class HelpServerTest {
    @Test
    public void helpIndexCompilesAndLoads() throws Exception {
        org.eclipse.help.internal.base.BaseHelpSystem.ensureWebappRunning();
        String url = "http://" + org.eclipse.help.internal.server.WebappManager.getHost()
                + ":" + org.eclipse.help.internal.server.WebappManager.getPort() + "/help/index.jsp";
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        int status = connection.getResponseCode();
        var input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body;
        try (input) { body = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
        finally { connection.disconnect(); }

        assertEquals(body, 200, status);
        assertTrue(body.contains("<html"));
    }
}
