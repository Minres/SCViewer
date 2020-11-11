 
package com.minres.scviewer.e4.application.handlers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.MessageDialog;

public class UpdateHandler {
	@Execute
	public void execute(IProvisioningAgent agent, UISynchronize synchronize, IWorkbench workbench) {
		ProvisioningSession session = new ProvisioningSession(agent);
		UpdateOperation operation = new UpdateOperation(session);
		IStatus status = operation.resolveModal(null);
		if(status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			MessageDialog.openInformation(null, "Information", "Nothing to update");
		}
	}
		
}