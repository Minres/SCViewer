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
 * The Enum BitValue.
 */
public enum BitValue {

	/** The zero. */
	ZERO,

	/** The one. */
	ONE,

	/** The x. */
	X,

	/** The z. */
	Z;

	/** The Constant ORDINAL_TABLE. */
	private static final BitValue[] ORDINAL_TABLE = BitValue.values();

	/**
	 * From char.
	 *
	 * @param c the c
	 * @return the bit value
	 */
	public static BitValue fromChar(char c) {
		switch (c) {
		case '0':
			return ZERO;
		case '1':
			return ONE;
		case 'x':
		case 'X':
			return X;
		case 'z':
		case 'Z':
			return Z;
		default:
			throw new NumberFormatException("unknown digit " + c);
		}
	}

	/**
	 * To char.
	 *
	 * @return the char
	 */
	public char toChar() {
		switch (this) {
		case ZERO:
			return '0';
		case ONE:
			return '1';
		case X:
			return 'x';
		case Z:
			return 'z';
		}

		return ' '; // Unreachable?
	}

	/**
	 * From int.
	 *
	 * @param i the i
	 * @return the bit value
	 */
	public static BitValue fromInt(int i) {
		if (i == 0) {
			return ZERO;
		} else {
			return ONE;
		}
	}

	/**
	 * To int.
	 *
	 * @return the int
	 */
	public int toInt() {
		return (this == ONE) ? 1 : 0;
	}

	/**
	 * From ordinal.
	 *
	 * @param ord the ord
	 * @return the bit value
	 */
	public static BitValue fromOrdinal(int ord) {
		return ORDINAL_TABLE[ord];
	}

	/**
	 * Compare.
	 *
	 * @param other the other
	 * @return the int
	 */
	public int compare(BitValue other) {
		if (this == ONE && other == ZERO) {
			return 1;
		} else if (this == ZERO && other == ONE) {
			return -1;
		} else {
			// Either these are equal, or there is an X and Z, which match everything.
			return 0;
		}
	}
}