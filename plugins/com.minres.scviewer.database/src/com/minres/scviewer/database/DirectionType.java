/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
