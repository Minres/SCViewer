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
package com.minres.scviewer.database;

import java.io.File;
import java.util.Collection;

/**
 * The Interface IWaveformDbLoader.
 */
public interface IWaveformDbLoader {
//	static final String STREAM_ADDED = "StreamAdded";
//	
//	static final String GENERATOR_ADDED = "GeneratorAdded";
//	
//	static final String LOADING_FINISHED = "LoadingFinished";
//	/**
//	 * Attach a non-null PropertyChangeListener to this object.
//	 * 
//	 * @param l
//	 *            a non-null PropertyChangeListener instance
//	 * @throws IllegalArgumentException
//	 *             if the parameter is null
//	 */
//	public void addPropertyChangeListener(PropertyChangeListener l);
//
//	/**
//	 * Remove a PropertyChangeListener from this component.
//	 * 
//	 * @param l
//	 *            a PropertyChangeListener instance
//	 */
//	public void removePropertyChangeListener(PropertyChangeListener l) ;

	/**
	 * Load.
	 *
	 * @param db  the db
	 * @param inp the inp
	 * @return true, if successful
	 * @throws InputFormatException the input format exception
	 */
	public boolean load(IWaveformDb db, File inp) throws InputFormatException;

	/**
	 * Gets the max time.
	 *
	 * @return the max time
	 */
	public Long getMaxTime();

	/**
	 * Gets the all waves.
	 *
	 * @return the all waves
	 */
	public Collection<IWaveform> getAllWaves();

	/**
	 * Gets the all relation types.
	 *
	 * @return the all relation types
	 */
	public Collection<RelationType> getAllRelationTypes();

	/**
	 * Dispose.
	 */
	public void dispose();
}
