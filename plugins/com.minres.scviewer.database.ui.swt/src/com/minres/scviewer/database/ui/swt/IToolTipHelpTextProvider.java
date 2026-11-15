/*******************************************************************************
 * Copyright (c) 2020-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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