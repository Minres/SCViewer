/*******************************************************************************
 * Copyright (c) 2020-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
