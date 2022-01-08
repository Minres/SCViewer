package com.minres.scviewer.e4.application.handlers;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.server.WebappManager;

public class HelpContentsHandler {
    @Execute
    public void execute() throws Exception {
        BaseHelpSystem.ensureWebappRunning();
        String helpURL = "http://" //$NON-NLS-1$
                + WebappManager.getHost() + ":" //$NON-NLS-1$
                + WebappManager.getPort() + "/help/index.jsp"; //$NON-NLS-1$
        BaseHelpSystem.getHelpBrowser(false).displayURL(helpURL);
    }
}