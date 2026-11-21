/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.wb.swt.SWTResourceManager;
import org.osgi.framework.BundleContext;

public class DatabaseUiPlugin extends Plugin {

	@Override
	public void start(BundleContext context) throws Exception {
		getLog().log(new Status(IStatus.OK, "org.eclipse.e4.core", "Starting org.eclipse.e4.core bundle..."));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		SWTResourceManager.dispose();
		getLog().log(new Status(IStatus.OK, "org.eclipse.e4.core", "Stopping org.eclipse.e4.core bundle..."));
	}
}