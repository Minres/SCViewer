/*******************************************************************************
 * Copyright (c) 2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ui;

public interface IWaveformZoom {
	
	long getMaxVisibleTime();

	long getMinVisibleTime();

	void setMinVisibleTime(long scale);

	long getMaxTime();

	long getScale();

	void setScale(long factor);

	void setVisibleRange(long startTime, long endTime);
	
	void centerAt(long time);
	
	void zoom(ZoomKind kind);

	String timeToString(long time);
}
