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
package com.minres.scviewer.database;

/**
 * The Enum DirectionType.
 */
public enum DirectionType {
	IMPLICIT,
	INPUT,
	OUTPUT,
	INOUT,
	BUFFER,
	LINKAGE;

	public String toString() {
		switch(this) {
		case INPUT:   return "I";
		case OUTPUT:  return "O";
		case INOUT:   return "IO";
		case BUFFER:  return "B";
		case LINKAGE: return "L";
		default:      return "";
		}
	}
}
