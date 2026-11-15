/*******************************************************************************
 * Copyright (c) 2020 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

public interface IWaveformStyleProvider {
	
	Font getNameFont();

	Font getNameFontHighlite();
	
	int getTrackHeight();

	Color getColor(WaveformColors type);

	Color[] computeColor(String name);
}
