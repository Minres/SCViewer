/*******************************************************************************
 * Copyright (c) 2015, 2020 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * The Interface IHierNode.
 */
public interface IHierNode extends Comparable<IHierNode>{
	
	/**
	 * Attach a non-null PropertyChangeListener to this object.
	 * 
	 * @param l
	 *            a non-null PropertyChangeListener instance
	 * @throws IllegalArgumentException
	 *             if the parameter is null
	 */
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	 * Remove a PropertyChangeListener from this component.
	 * 
	 * @param l
	 *            a PropertyChangeListener instance
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) ;

	/**
	 * Gets the full name.
	 *
	 * @return the full name
	 */
	public String getFullName();
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name);
	
	/**
	 * Sets the parent.
	 *
	 * @param parent the new parent
	 */
	public void setParent(IHierNode parent);

	/**
	 * Gets the child nodes.
	 *
	 * @return the child nodes
	 */
	public List<IHierNode> getChildNodes();
	
	/**
	 * Derive waveform.
	 *
	 * @return the i derived waveform or null if none could be created
	 */
	public IDerivedWaveform deriveWaveform();

}
