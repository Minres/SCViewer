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
package com.minres.scviewer.e4.application;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.eclipse.e4.core.services.log.Logger;

import com.minres.scviewer.e4.application.options.Options;
import com.minres.scviewer.e4.application.options.Options.Multiplicity;
import com.minres.scviewer.e4.application.options.Options.Separator;

/**
 * This implementation contains e4 LifeCycle annotated methods.<br />
 * There is a corresponding entry in <em>plugin.xml</em> (under the
 * <em>org.eclipse.core.runtime.products' extension point</em>) that references
 * this class.
 **/
public class E4LifeCycle {

	@Inject private Logger logger;
	
	/**
	 * Post construct.
	 *
	 * @param eventBroker the event broker
	 */
	@PostConstruct
	private static void postConstruct(final IEventBroker eventBroker) {
	}

	/**
	 * Post context create.  Open a database if given on command line using the OpenViewHandler
	 *
	 * @param appContext the app context
	 * @param eventBroker the event broker
	 */
	@PostContextCreate
	void postContextCreate(IApplicationContext appContext, final IEventBroker eventBroker,  
			final IEclipseContext workbenchContext) {
		
		final String[] args = (String[])appContext.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		final Options opt = new Options(args, 0, Integer.MAX_VALUE);
		opt.getSet()
			.addOption("clearPersistedState", Multiplicity.ZERO_OR_ONE)
			.addOption("c", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		if (!opt.check(Options.DEFAULT_SET, true, false)) {
			logger.error(opt.getCheckErrors());
			System.exit(1);
		}

		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				Location instanceLocation = Platform.getInstanceLocation();
				try {
					boolean isLocked = instanceLocation.isLocked();
					if(isLocked)
						instanceLocation.release();
				} catch (IOException e) { }
				if(!opt.getSet().getData().isEmpty()) {
					Display.getCurrent().timerExec (100, () -> {
						MApplication app= workbenchContext.get(MApplication.class);
						EModelService modelService = workbenchContext.get(EModelService.class);
						EPartService partService= workbenchContext.get(EPartService.class);
						MPart part = partService .createPart("com.minres.scviewer.e4.application.partdescriptor.waveformviewer"); //$NON-NLS-1$
						part.setLabel(opt.getSet().getData().get(0));
						MPartStack partStack = (MPartStack)modelService.find("org.eclipse.editorss", app); //$NON-NLS-1$
						partStack.getChildren().add(part);
						partService.showPart(part, PartState.CREATE);
						partService.showPart(part, PartState.ACTIVATE);
						IEclipseContext ctx = part.getContext();
						ctx.modify("input", opt.getSet().getData());
						String confFile =opt.getSet().isSet("c")?opt.getSet().getOption("c").getResultValue(0):"";
						ctx.modify("config", confFile); //$NON-NLS-1$
					});
				}
			}
		});
	}
	
	/**
	 * Pre save.
	 *
	 * @param workbenchContext the workbench context
	 */
	@PreSave
	void preSave(IEclipseContext workbenchContext) {
		/* nothing to be done here */
	}

	/**
	 * Process additions.
	 *
	 * @param workbenchContext the workbench context
	 */
	@ProcessAdditions
	void processAdditions(IEclipseContext workbenchContext) {
		/* nothing to be done here */
	}

	/**
	 * Process removals.
	 *
	 * @param workbenchContext the workbench context
	 */
	@ProcessRemovals
	void processRemovals(IEclipseContext workbenchContext) {
		/* nothing to be done here */
	}
}
