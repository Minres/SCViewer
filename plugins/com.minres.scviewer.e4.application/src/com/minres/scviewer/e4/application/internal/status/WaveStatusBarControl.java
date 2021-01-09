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
package com.minres.scviewer.e4.application.internal.status;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.minres.scviewer.e4.application.Messages;

/**
 * The Class WaveStatusBarControl.
 */
public class WaveStatusBarControl extends StatusBarControl {

	/** The Constant ZOOM_LEVEL. */
	public static final String ZOOM_LEVEL="ZoomLevelUpdate"; //$NON-NLS-1$
	
	/** The Constant CURSOR_TIME. */
	public static final String CURSOR_TIME="CursorPosUpdate"; //$NON-NLS-1$
	
	/** The Constant MARKER_TIME. */
	public static final String MARKER_TIME="MarkerPosUpdate"; //$NON-NLS-1$
	
	/** The Constant MARKER_DIFF. */
	public static final String MARKER_DIFF="MarlerDiffUpdate"; //$NON-NLS-1$

	/** The model service. */
	@Inject
	EModelService modelService;


	/** The zoom contribution. */
	StatusLineContributionItem cursorContribution, markerContribution, markerDiffContribution, zoomContribution;

	/**
	 * Instantiates a new wave status bar control.
	 *
	 * @param sync the sync
	 */
	@Inject
	public WaveStatusBarControl(UISynchronize sync) {
		super(sync);
				
		cursorContribution = new StatusLineContributionItem(Messages.WaveStatusBarControl_5, true, 20);
		markerContribution = new StatusLineContributionItem(Messages.WaveStatusBarControl_6, true, 20);
		markerDiffContribution = new StatusLineContributionItem(Messages.WaveStatusBarControl_7, true, 20);
		zoomContribution = new StatusLineContributionItem(Messages.WaveStatusBarControl_8, true, 8);
		manager.appendToGroup(StatusLineManager.BEGIN_GROUP,cursorContribution);
		manager.appendToGroup(StatusLineManager.BEGIN_GROUP,markerContribution);
		manager.appendToGroup(StatusLineManager.BEGIN_GROUP,markerDiffContribution);
		manager.appendToGroup(StatusLineManager.BEGIN_GROUP, zoomContribution);
	}

	/**
	 * Sets the selection.
	 *
	 * @param selection the new selection
	 */
	@Inject
	public void setSelection(@Named(IServiceConstants.ACTIVE_SELECTION)@Optional IStructuredSelection selection){
		if(manager!=null && selection!=null){
			switch(selection.size()){
			case 0:
				manager.setMessage(""); //$NON-NLS-1$
				break;
			case 1:
				manager.setMessage(selection.getFirstElement().getClass().getSimpleName()+Messages.WaveStatusBarControl_10);
				break;
			default:
				manager.setMessage(""+selection.size()+Messages.WaveStatusBarControl_12); //$NON-NLS-1$
				break;
			}
		}
	}

	/**
	 * Gets the zoom event.
	 *
	 * @param text the text
	 * @return the zoom event
	 */
	@Inject @Optional
	public void  getZoomEvent(@UIEventTopic(ZOOM_LEVEL) String text) {
		zoomContribution.setText(text);
	} 

	/**
	 * Gets the cursor event.
	 *
	 * @param text the text
	 * @return the cursor event
	 */
	@Inject @Optional
	public void  getCursorEvent(@UIEventTopic(CURSOR_TIME) String text) {
		cursorContribution.setText(text);
	} 

	/**
	 * Gets the marker event.
	 *
	 * @param text the text
	 * @return the marker event
	 */
	@Inject @Optional
	public void  getMarkerEvent(@UIEventTopic(MARKER_TIME) String text) {
		markerContribution.setText(text);
	} 

	/**
	 * Gets the diff event.
	 *
	 * @param text the text
	 * @return the diff event
	 */
	@Inject @Optional
	public void  getDiffEvent(@UIEventTopic(MARKER_DIFF) String text) {
		markerDiffContribution.setText(text);
	} 

}