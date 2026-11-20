/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.internal.status;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.widgets.Composite;
import org.osgi.service.prefs.PreferencesService;

import com.minres.scviewer.e4.application.AppModelId;

/**
 * The Class StatusBarControl.
 */
public class StatusBarControl {

	/** The Constant STATUS_UPDATE. */
	public static final String STATUS_UPDATE="StatusUpdate"; //$NON-NLS-1$

	/** The model service. */
	@Inject	EModelService modelService;

	/** The osgi preverences. */
	@Inject	@Optional PreferencesService osgiPreverences;

	/** The manager. */
	protected StatusLineManager manager;

	/** The monitor. */
	private final java.util.Set<JobProgress> monitors = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicBoolean updatePending = new java.util.concurrent.atomic.AtomicBoolean();
    private static StatusBarControl providerOwner;
    private volatile boolean disposed;
    private org.eclipse.swt.widgets.Display display;
	
	/**
	 * Instantiates a new status bar control.
	 *
	 * @param sync the sync
	 */
	@Inject
	public StatusBarControl(UISynchronize sync) {
		manager = new StatusLineManager();
		manager.update(true);
	}

	/**
	 * Creates the widget.
	 *
	 * @param parent the parent
	 * @param toolControl the tool control
	 */
	@PostConstruct
	void createWidget(Composite parent, MToolControl toolControl) {
		if (toolControl.getElementId().equals(AppModelId.TOOLCONTROL_ORG_ECLIPSE_UI_STATUSLINE)) { //$NON-NLS-1$
			createStatusLine(parent, toolControl);
		} else if (toolControl.getElementId().equals(AppModelId.TOOLCONTROL_ORG_ECLIPSE_UI_HEAPSTATUS)) { //$NON-NLS-1$
			new HeapStatus(parent, osgiPreverences.getSystemPreferences());
		}
	}

	/**
	 * Destroy.
	 */
	@PreDestroy
	void destroy() {
        disposed = true;
        if (providerOwner == this) {
            Job.getJobManager().setProgressProvider(null);
            providerOwner = null;
        }
        monitors.clear();
		if (manager != null) {
			manager.dispose();
			manager = null;
		}
	}

	/**
	 * Creates the status line.
	 *
	 * @param parent the parent
	 * @param toolControl the tool control
	 */
	private void createStatusLine(Composite parent, MToolControl toolControl) {
		//		IEclipseContext context = modelService.getContainingContext(toolControl);
		manager.createControl(parent);
        display = parent.getDisplay();
        manager.setCancelEnabled(false);
        providerOwner = this;
        Job.getJobManager().setProgressProvider(new ProgressProvider() {
            @Override public IProgressMonitor createMonitor(Job job) {
                JobProgress monitor = new JobProgress(StatusBarControl.this::requestProgressUpdate);
                if (disposed) return monitor;
                monitors.add(monitor);
                job.addJobChangeListener(new JobChangeAdapter() {
                    @Override public void done(IJobChangeEvent event) {
                        monitors.remove(monitor);
                        requestProgressUpdate();
                        event.getJob().removeJobChangeListener(this);
                    }
                });
                return monitor;
            }
        });
	}

	/**
	 * Gets the status event.
	 *
	 * @param text the text
	 * @return the status event
	 */
	@Inject @Optional
	public void  getStatusEvent(@UIEventTopic(STATUS_UPDATE) String text) {
		if(manager!=null ){
			manager.setMessage(text);
		}
	} 

    private void requestProgressUpdate() {
        if (disposed || !updatePending.compareAndSet(false, true)) return;
        com.minres.scviewer.database.ui.UiDispatch.async(display, () -> {
            updatePending.set(false);
            if (disposed || manager == null || manager.getControl().isDisposed()) return;
            var snapshots = monitors.stream().map(JobProgress::snapshot).filter(p -> !p.done()).toList();
            IProgressMonitor bar = manager.getProgressMonitor();
            if (snapshots.isEmpty()) { bar.done(); return; }
            String title = snapshots.get(0).name();
            if (snapshots.size() > 1) title += " (+" + (snapshots.size() - 1) + " tasks)";
            bar.beginTask(title, 1000);
            int work = (int) snapshots.stream().mapToDouble(p -> p.total() > 0
                    ? Math.min(1, p.work() / p.total()) * 1000 : 0).average().orElse(0);
            bar.worked(work);
        });
    }
}
