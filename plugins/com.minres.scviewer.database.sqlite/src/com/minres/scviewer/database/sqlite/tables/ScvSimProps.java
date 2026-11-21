/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.sqlite.tables;

public class ScvSimProps {
	
	private long time_resolution;

	public ScvSimProps() {
		super();
	}

	public long getTime_resolution() {
		return time_resolution;
	}

	public void setTime_resolution(long time_resolution) {
		this.time_resolution = time_resolution;
	}

}
