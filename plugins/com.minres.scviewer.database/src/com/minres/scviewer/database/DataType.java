/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
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
 * The Enum DataType.
 */
public enum DataType {
	/** The boolean. */
	BOOLEAN,                      // bool
	/** The enumeration. */
	ENUMERATION,                  // enum
	/** The integer. */
	INTEGER,                      // char, short, int, long, long long, sc_int, sc_bigint
	/** The unsigned. */
	UNSIGNED,                     // unsigned { char, short, int, long, long long }, sc_uint, sc_biguint
	/** The floating point number. */
	FLOATING_POINT_NUMBER,        // float, double
	/** The bit vector. */
	BIT_VECTOR,                   // sc_bit, sc_bv
	/** The logic vector. */
	LOGIC_VECTOR,                 // sc_logic, sc_lv
	/** The fixed point integer. */
	FIXED_POINT_INTEGER,          // sc_fixed
	/** The unsigned fixed point integer. */
	UNSIGNED_FIXED_POINT_INTEGER, // sc_ufixed
	/** The pointer. */
	POINTER,                      // T*
	/** The string. */
	STRING,                       // string, std::string
	/** The time. */
	TIME,                         // sc_time
	/** The void type. */
	NONE
}
