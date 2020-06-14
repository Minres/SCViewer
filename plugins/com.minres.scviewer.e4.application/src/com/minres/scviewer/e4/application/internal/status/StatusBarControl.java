/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
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
import org.eclipse.core.runtime.NullProgressMonitor;
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

	/** The sync. */
	private final UISynchronize sync;

	/** The manager. */
	protected StatusLineManager manager;

	/** The monitor. */
	private SyncedProgressMonitor monitor;
	
	/**
	 * Instantiates a new status bar control.
	 *
	 * @param sync the sync
	 */
	@Inject
	public StatusBarControl(UISynchronize sync) {
		this.sync=sync;
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
		monitor=new SyncedProgressMonitor(manager.getProgressMonitor());
		Job.getJobManager().setProgressProvider(new ProgressProvider() {
			@Override
			public IProgressMonitor createMonitor(Job job) {
				return monitor.addJob(job);
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

	/**
	 * The Class SyncedProgressMonitor.
	 */
	private final class SyncedProgressMonitor extends NullProgressMonitor {

		/** The progress bar. */
		private IProgressMonitor progressBar;

		/**
		 * Instantiates a new synced progress monitor.
		 *
		 * @param iProgressMonitor the progress bar
		 */
		public SyncedProgressMonitor(IProgressMonitor iProgressMonitor) {
			super();
			this.progressBar = iProgressMonitor;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.NullProgressMonitor#beginTask(java.lang.String, int)
		 */
		@Override
		public void beginTask(final String name, final int totalWork) {
			sync.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressBar.beginTask(name, totalWork);
				}
			});
		}

		/**
		 * This implementation does nothing.
		 * Subclasses may override this method to do something
		 * with the name of the task.
		 * 
		 * @see IProgressMonitor#setTaskName(String)
		 */
		@Override
		public void setTaskName(String name) {
			sync.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressBar.setTaskName(name);
				}
			});
		}

		/**
		 * This implementation does nothing.
		 * Subclasses may override this method to do interesting
		 * processing when a subtask begins.
		 * 
		 * @see IProgressMonitor#subTask(String)
		 */
		@Override
		public void subTask(String name) {
			sync.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressBar.subTask(name);
				}
			});
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.NullProgressMonitor#worked(int)
		 */
		@Override
		public void worked(final int work) {
			sync.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressBar.worked(work);
				}
			});
		}
		@Override
		public void done() {
			sync.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressBar.done();
				}
			});
		}

		/**
		 * Adds the job.
		 *
		 * @param job the job
		 * @return the i progress monitor
		 */
		public IProgressMonitor addJob(Job job){
			if(job != null){
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						sync.asyncExec(new Runnable() {
							@Override
							public void run() {
								if(event.getResult()==null) return;
								if(!event.getResult().isOK())
									progressBar.done();
							}
						});
						// clean-up
						event.getJob().removeJobChangeListener(this);
					}
				});
			}
			return this;
		}
	}
}