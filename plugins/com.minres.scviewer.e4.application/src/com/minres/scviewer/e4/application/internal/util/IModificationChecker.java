/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.internal.util;

/**
 * The Interface IModificationChecker. Allows to trigger a check independent of the timer
 */
public interface IModificationChecker {

	/**
	 * Check.
	 */
	public void check();

}
