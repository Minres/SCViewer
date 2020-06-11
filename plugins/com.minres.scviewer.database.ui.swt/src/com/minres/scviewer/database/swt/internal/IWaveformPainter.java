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
package com.minres.scviewer.database.swt.internal;

import com.minres.scviewer.database.ui.TrackEntry;

public interface IWaveformPainter extends IPainter {
	
	public int getHeight();

	public int getVerticalOffset();

	public TrackEntry getTrackEntry();

}