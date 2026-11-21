/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
