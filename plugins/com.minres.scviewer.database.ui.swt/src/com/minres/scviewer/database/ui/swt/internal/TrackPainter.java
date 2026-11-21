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

public abstract class TrackPainter implements IWaveformPainter {

	protected boolean even;

	protected TrackEntry trackEntry;

	public TrackPainter(TrackEntry trackEntry, boolean even) {
		this.trackEntry = trackEntry;
		this.even=even;
	}

	public int getHeight() {
		return trackEntry.height;
	}

	public int getVerticalOffset() {
		return trackEntry.vOffset;
	}

	public boolean isEven() {
		return even;
	}

	public TrackEntry getTrackEntry() {
		return trackEntry;
	}

}
