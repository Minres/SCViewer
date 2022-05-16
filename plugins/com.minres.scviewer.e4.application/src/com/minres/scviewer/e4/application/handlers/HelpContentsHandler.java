package com.minres.scviewer.e4.application.handlers;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.server.WebappManager;

public class HelpContentsHandler {

	static final String WINDOW_ID="com.minres.scviewer.e4.application.window.help_content"; //$NON-NLS-1$

	@CanExecute
	public boolean canExecute(MApplication app) {
		return !app.getChildren().stream().filter(e -> e.getElementId().equals(WINDOW_ID)).findFirst().isPresent();
	}
	
	@Execute
	public void execute(MApplication app, EModelService modelService /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
        BaseHelpSystem.ensureWebappRunning();
        String helpURL = "http://" //$NON-NLS-1$
                + WebappManager.getHost() + ":" //$NON-NLS-1$
                + WebappManager.getPort() + "/help/index.jsp"; //$NON-NLS-1$
        // BaseHelpSystem.getHelpBrowser(false).displayURL(helpURL);
		MWindow newWin = (MWindow)modelService.cloneSnippet(app, WINDOW_ID, null);
		final IEclipseContext ctx=app.getContext();
		if(ctx.containsKey("help_url"))
			ctx.remove("help_url");
		ctx.modify("help_url", helpURL);
		app.getChildren().add(newWin);
	}
}