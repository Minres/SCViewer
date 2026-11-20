/*******************************************************************************
 * Copyright (c) 2020-2022 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
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

	private final java.util.concurrent.atomic.AtomicBoolean updating = new java.util.concurrent.atomic.AtomicBoolean();

	@Execute
	public void execute(IProvisioningAgent agent, UISynchronize sync, IWorkbench workbench) {
        if (!updating.compareAndSet(false, true)) return;
        java.util.concurrent.atomic.AtomicBoolean scheduled = new java.util.concurrent.atomic.AtomicBoolean();
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				update(agent, monitor, sync, workbench, scheduled);
			}
		};
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally { if (!scheduled.get()) updating.set(false); }
	}

	private IStatus update(IProvisioningAgent agent, IProgressMonitor monitor, UISynchronize sync,
			IWorkbench workbench, java.util.concurrent.atomic.AtomicBoolean scheduled) {
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
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
                                    updating.set(false);
									if (event.getResult().isOK()) {
										sync.asyncExec(new Runnable() {

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
										sync.asyncExec(() -> {
											MessageDialog.openInformation(null, Messages.UpdateHandler_11,
													event.getResult().getMessage());
										});
									}
								}
							});
                            scheduled.set(true);
							provisioningJob.schedule();
						} else {
							cancelled.set(true);
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
				cancelled.set(true);
			}
		}
		if (cancelled.get()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
}
