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
