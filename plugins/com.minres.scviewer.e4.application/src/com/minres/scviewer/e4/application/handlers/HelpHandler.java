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
package com.minres.scviewer.e4.application.handlers;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IHelp;
import org.eclipse.help.standalone.Help;
import org.eclipse.osgi.service.datalocation.Location;

public class HelpHandler {

	static final String DIALOG_ID="com.minres.scviewer.e4.application.dialog.onlinehelp"; //$NON-NLS-1$
	static final String WINDOW_ID="com.minres.scviewer.e4.application.window.help"; //$NON-NLS-1$
	
	@Execute
	public void execute(MApplication app, /*MWindow window,*/ EModelService ms /*@Named("mdialog01.dialog.0") MDialog dialog*/) {
//		MPart mel = (MPart) ms.find(DIALOG_ID, app); //$NON-NLS-1$
//		mel.setToBeRendered(true);
//		mel.setToBeRendered(false);
		try {
			File installDir = Paths.get(Platform.getInstallLocation().getURL().toURI()).toFile();
			File instanceDir = Paths.get(Platform.getInstanceLocation().getURL().toURI()).toFile();
			Help helpSystem = new Help(new String[] {"-eclipseHome", installDir.getAbsolutePath(), "-data", instanceDir.getAbsolutePath()});
			helpSystem.start();
			helpSystem.displayHelp("/com.minres.scviewer.help/toc.xml");
		} catch (Exception e) {
			MUIElement w = ms.find(WINDOW_ID, app); 
			if(w!=null) w.setToBeRendered(true);
		} 
	}

}
