/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
