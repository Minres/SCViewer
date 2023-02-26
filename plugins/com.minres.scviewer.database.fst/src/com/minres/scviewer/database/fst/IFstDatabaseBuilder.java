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
package com.minres.scviewer.database.fst;

import com.minres.scviewer.database.BitVector;

/**
 * The Interface IVCDDatabaseBuilder. It allows to add VCD events into the database
 */
public interface IFstDatabaseBuilder {

	/**
	 * Enter module.
	 *
	 * @param tokenString the token string
	 */
	public void enterModule(String tokenString);

	/**
	 * Exit module.
	 */
	public void exitModule();

	/**
	 * New net.
	 *
	 * @param netName the net name
	 * @param i the index of the net, -1 if a new one, otherwise the id if the referenced
	 * @param width the width, -1 equals real, 0... is a bit vector
	 * @return the net id
	 */
	public Integer newNet(String netName, int i, int width) ;

	/**
	 * Gets the net width.
	 *
	 * @param intValue the net id
	 * @return the net width, -1 means a real-valued net
	 */
	public int getNetWidth(int netId);

	/**
	 * Append transition.
	 *
	 * @param netId the int value
	 * @param currentTime the current time in ps
	 * @param decodedValues the decoded values
	 */
	public void appendTransition(int netId, long currentTime, BitVector decodedValue);

	/**
	 * Append transition.
	 *
	 * @param netId the int value
	 * @param currentTime the current time in ps
	 * @param decodedValue the decoded values
	 */
	public void appendTransition(int netId, long currentTime, double decodedValue);

}
