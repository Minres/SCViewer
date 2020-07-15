/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.swt;

import org.eclipse.swt.widgets.Composite;

import com.minres.scviewer.database.swt.internal.WaveformView;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.IWaveformViewFactory;

public class WaveformViewFactory implements IWaveformViewFactory {

	@Override
	public IWaveformView createPanel(Composite parent) {
		return new WaveformView(parent);
	}

}
