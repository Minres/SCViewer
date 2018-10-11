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
package com.minres.scviewer.database.vcd;

import com.minres.scviewer.database.ISignalChangeReal;
import com.minres.scviewer.database.SignalChange;

public class VCDSignalChangeReal extends SignalChange implements ISignalChangeReal, Cloneable {

	private double value;

	public VCDSignalChangeReal(Long time, double value) {
		super(time);
		this.value=value;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value+"@"+getTime();
	}
}
