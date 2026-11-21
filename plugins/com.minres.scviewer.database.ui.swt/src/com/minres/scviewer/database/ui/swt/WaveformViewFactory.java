/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt;

import org.eclipse.swt.widgets.Composite;

import com.minres.scviewer.database.ui.IWaveformStyleProvider;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.IWaveformViewFactory;
import com.minres.scviewer.database.ui.swt.internal.WaveformView;

public class WaveformViewFactory implements IWaveformViewFactory {

	@Override
	public IWaveformView createPanel(Composite parent) {
		return new WaveformView(parent, new DefaultWaveformStyleProvider());
	}

	@Override
	public IWaveformView createPanel(Composite parent, IWaveformStyleProvider styleProvider) {
		return new WaveformView(parent, styleProvider);
	}

}
