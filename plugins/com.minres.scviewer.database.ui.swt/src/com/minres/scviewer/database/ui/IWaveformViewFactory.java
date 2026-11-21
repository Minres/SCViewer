/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

import org.eclipse.swt.widgets.Composite;

public interface IWaveformViewFactory {
	IWaveformView createPanel(Composite parent);

	IWaveformView createPanel(Composite parent, IWaveformStyleProvider styleProvider);
}
