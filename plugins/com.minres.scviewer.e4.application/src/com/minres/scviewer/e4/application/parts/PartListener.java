/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;

/**
 * The default implementation of a {@link IPartListener}.
 * The class that is interested in processing a part
 * event extends this class overriding the respective method, and the object created
 * with that class is registered with a component using the
 * component's <code>addPartListener<code> method. When
 * the part event occurs, that object's appropriate
 * method is invoked.
 *
 * @see PartEvent
 */
public class PartListener implements IPartListener {
	
	/* (non-Javadoc)
	 * @see org.eclipse.e4.ui.workbench.modeling.IPartListener#partBroughtToTop(org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	@Override
	public void partBroughtToTop(MPart part) {}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.ui.workbench.modeling.IPartListener#partActivated(org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	@Override
	public void partActivated(MPart part) {}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.ui.workbench.modeling.IPartListener#partDeactivated(org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	@Override
	public void partDeactivated(MPart part) {}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.ui.workbench.modeling.IPartListener#partHidden(org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	@Override
	public void partHidden(MPart part) {}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.ui.workbench.modeling.IPartListener#partVisible(org.eclipse.e4.ui.model.application.ui.basic.MPart)
	 */
	@Override
	public void partVisible(MPart part) {}
}