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
package com.minres.scviewer.database.tx;

import com.minres.scviewer.database.RelationType;

/**
 * The Interface ITxRelation.
 */
public interface ITxRelation {

	/**
	 * Gets the relation type.
	 *
	 * @return the relation type
	 */
	RelationType getRelationType();

	/**
	 * Gets the source.
	 *
	 * @return the source
	 */
	ITx getSource();

	/**
	 * Gets the target.
	 *
	 * @return the target
	 */
	ITx getTarget();
}
