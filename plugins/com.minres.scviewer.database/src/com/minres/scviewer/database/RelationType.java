/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.io.Serializable;

/**
 * The Class RelationType.
 */
public class RelationType implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6394859077558971735L;

	/** The name. */
	private String name;

	/**
	 * Instantiates a new relation type.
	 *
	 * @param name the name
	 */
	RelationType(String name) {
		super();
		this.name = name;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	public String toString() {
		return name;
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RelationType)
			return name.equals(((RelationType) obj).name);
		else
			return false;
	}
}
