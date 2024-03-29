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

import java.io.File;
import java.util.List;

/**
 * The Interface IWaveformDb.
 */
public interface IWaveformDb extends IHierNode {

	/**
	 * the time scale of the database. This is the exponent of the value i.e. -12 is ps
	 */
	public static final int databaseTimeScale = -15;
	
	/**
	 * Gets the max time.
	 *
	 * @return the max time
	 */
	public long getMaxTime();

	/**
	 * Gets the stream by name.
	 *
	 * @param name the name
	 * @return the stream by name
	 */
	public IWaveform getStreamByName(String name);

	/**
	 * Gets the all waves.
	 *
	 * @return the all waves
	 */
	public List<IWaveform> getAllWaves();

	/**
	 * Gets the all relation types.
	 *
	 * @return the all relation types
	 */
	public List<RelationType> getAllRelationTypes();

	/**
	 * Load.
	 *
	 * @param inp the inp
	 * @return true, if successful
	 */
	public boolean load(File inp);

	/**
	 * Checks if is loaded.
	 *
	 * @return true, if is loaded
	 */
	public boolean isLoaded();

	/**
	 * close an open database.
	 *
	 * @param inp the inp
	 * @return true, if successful
	 */
	public void close();


}
