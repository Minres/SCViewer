/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/


package com.minres.scviewer.e4.application.internal.util;

import java.io.File;
import java.util.List;

/**
 * Listener interested in {@link File} changes.
 * 
 */
public interface IFileChangeListener {
	
	/**
	 * Invoked when a file changes.
	 *
	 * @param file the file
	 */
	public void fileChanged(List<File> file);
}