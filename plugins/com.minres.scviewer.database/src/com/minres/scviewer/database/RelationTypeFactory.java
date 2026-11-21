/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for creating RelationType objects.
 */
public class RelationTypeFactory {

	/**
	 * Creates the.
	 *
	 * @param name the name
	 * @return the relation type
	 */
	public static RelationType create(String name) {
		return registry.computeIfAbsent(name, RelationType::new);
	}

	/**
	 * Instantiates a new relation type factory.
	 */
	private RelationTypeFactory() {
	}

	/** The registry. */
	private static ConcurrentHashMap<String, RelationType> registry = new ConcurrentHashMap<>();

}
