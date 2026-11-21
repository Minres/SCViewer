/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui.swt.internal;

import com.minres.scviewer.database.ui.TrackEntry;

public interface IWaveformPainter extends IPainter {
	
	public int getHeight();

	public int getVerticalOffset();

	public TrackEntry getTrackEntry();

}