/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
