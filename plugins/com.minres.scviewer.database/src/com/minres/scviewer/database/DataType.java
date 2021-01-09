/*******************************************************************************
 * Copyright (c) 2015 - 2020 MINRES Technologies GmbH and others.
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
	BOOLEAN,
	/** The enumeration. */
	// bool
	ENUMERATION,
	/** The integer. */
	// enum
	INTEGER,
	/** The unsigned. */
	// char, short, int, long, long long, sc_int, sc_bigint
	UNSIGNED, // unsigned { char, short, int, long, long long }, sc_uint,
	/** The floating point number. */
	// sc_biguint
	FLOATING_POINT_NUMBER,
	/** The bit vector. */
	// float, double
	BIT_VECTOR,
	/** The logic vector. */
	// sc_bit, sc_bv
	LOGIC_VECTOR,
	/** The fixed point integer. */
	// sc_logic, sc_lv
	FIXED_POINT_INTEGER,
	/** The unsigned fixed point integer. */
	// sc_fixed
	UNSIGNED_FIXED_POINT_INTEGER,
	/** The record. */
	// sc_ufixed
	RECORD,
	/** The pointer. */
	// struct/class
	POINTER,
	/** The array. */
	// T*
	ARRAY,
	/** The string. */
	// T[N]
	STRING // string, std::string
}
