/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Class HierNode.
 */
public class HierNode implements IHierNode {

	/** The name. */
	protected volatile String name;

	/** The parent. */
	protected volatile IHierNode parent = null;

	/** The childs. */
	protected List<IHierNode> childNodes = Collections.synchronizedList(new ArrayList<>());

	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * Instantiates a new hier node.
	 *
	 * @param name the name
	 */
	public HierNode(String name) {
		this.name = name;
	}

	/**
	 * Instantiates a new hier node.
	 *
	 * @param name   the name
	 * @param parent the parent
	 */
	public HierNode(String name, IHierNode parent) {
		this.name = name;
		this.parent = parent;
	}

	/**
	 * Instantiates a new hier node.
	 */
	public HierNode() {
	}

	/**
	 * Adds the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * Removes the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	/**
	 * Gets the full hierarchical name.
	 *
	 * @return the full name
	 */
	@Override
	public String getFullName() {
		if (parent != null)
			return parent.getFullName() + "." + name;
		else
			return name;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	@Override
	public IHierNode getParent() {
		return parent;
	}

	/**
	 * Sets the parent.
	 *
	 * @param parent the new parent
	 */
	@Override
	public void setParent(IHierNode parent) {
		this.parent = parent;
	}

	/**
	 * Gets the child nodes.
	 *
	 * @return the child nodes
	 */
	@Override
	public List<IHierNode> getChildNodes() {
		synchronized (childNodes) { return List.copyOf(childNodes); }
	}

	/**
	 * Adds the child.
	 *
	 * @param child the child
	 */
	@Override
	public void addChild(IHierNode child) {
		synchronized (childNodes) {
			if (!childNodes.contains(child)) {
				child.setParent(this);
				childNodes.add(child);
			}
		}
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(IHierNode o) {
		return getFullName().compareTo(o.getFullName());
	}

	/**
	 * Derive waveform.
	 *
	 * @return the i derived waveform
	 */
	@Override
	public IDerivedWaveform deriveWaveform() {
		return null;
	}

}
