/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt;

import org.eclipse.swt.widgets.Widget;

public interface IToolTipHelpTextProvider {
	/**
	 * Get help text
	 * @param widget the widget that is under help
	 * @return a help text string
	 */
	public String getHelpText(Widget widget);
}