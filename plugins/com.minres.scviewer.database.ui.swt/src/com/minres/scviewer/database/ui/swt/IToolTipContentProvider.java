/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public interface IToolTipContentProvider {
	
	public boolean createContent(Composite parent, Point pt);

}