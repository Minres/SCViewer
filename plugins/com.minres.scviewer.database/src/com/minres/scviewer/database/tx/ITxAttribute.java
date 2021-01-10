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
package com.minres.scviewer.database.tx;

/**
 * The Interface ITxAttribute.
 */
public interface ITxAttribute extends ITxAttributeType {

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public Object getValue();
}
