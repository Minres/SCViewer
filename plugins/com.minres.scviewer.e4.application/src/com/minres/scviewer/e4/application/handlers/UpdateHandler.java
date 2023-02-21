package com.minres.scviewer.e4.application.handlers;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.minres.scviewer.e4.application.Messages;

public class UpdateHandler {

	boolean cancelled = false;

	@Execute
	public void execute(IProvisioningAgent agent, UISynchronize sync, IWorkbench workbench) {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				update(agent, monitor, sync, workbench);
			}
		};
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private IStatus update(IProvisioningAgent agent, IProgressMonitor monitor, UISynchronize sync,
			IWorkbench workbench) {
		ProvisioningSession session = new ProvisioningSession(agent);
		UpdateOperation operation = new UpdateOperation(session);
		try {
			operation.getProvisioningContext().setArtifactRepositories(new URI(Messages.UpdateHandler_URI));
			operation.getProvisioningContext().setMetadataRepositories(new URI(Messages.UpdateHandler_URI));
		} catch (URISyntaxException e) {
		}
		SubMonitor sub = SubMonitor.convert(monitor, Messages.UpdateHandler_2, 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			sync.syncExec(() -> {
				MessageDialog.openInformation(null, Messages.UpdateHandler_10, Messages.UpdateHandler_3);
			});
			return Status.CANCEL_STATUS;
		} else {
			ProvisioningJob provisioningJob = operation.getProvisioningJob(sub.newChild(100));
			if (provisioningJob != null) {
				sync.syncExec(new Runnable() {
					@Override
					public void run() {
						boolean performUpdate = MessageDialog.openQuestion(null, Messages.UpdateHandler_4,
								Messages.UpdateHandler_5);
						if (performUpdate) {
							provisioningJob.addJobChangeListener(new JobChangeAdapter() {
								@Override
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										sync.syncExec(new Runnable() {

											@Override
											public void run() {
												boolean restart = MessageDialog.openQuestion(null,
														Messages.UpdateHandler_6, Messages.UpdateHandler_7);
												if (restart) {
													workbench.restart();
												}
											}
										});
									} else {
										sync.syncExec(() -> {
											MessageDialog.openInformation(null, Messages.UpdateHandler_11,
													event.getResult().getMessage());
										});
										cancelled = true;
									}
								}
							});
							provisioningJob.schedule();
						} else {
							cancelled = true;
						}
					}
				});
			} else {
				if (operation.hasResolved()) {
					sync.syncExec(() -> {
						MessageDialog.openInformation(null, Messages.UpdateHandler_11,
								Messages.UpdateHandler_8 + operation.getResolutionResult());
					});
				} else {
					sync.syncExec(() -> {
						MessageDialog.openInformation(null, Messages.UpdateHandler_11, Messages.UpdateHandler_9);
					});
				}
				cancelled = true;
			}
		}
		if (cancelled) {
			cancelled = false;
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
}
