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
package com.minres.scviewer.e4.application.preferences;

import com.minres.scviewer.e4.application.Constants;

/**
 * The Class PreferenceConstants for the preferences dialog & setting.
 */
public class PreferenceConstants {

	/** The Constant PREFERENCES_SCOPE. */
	public static final String PREFERENCES_SCOPE=Constants.PLUGIN_ID; //$NON-NLS-1$
	
	/** The Constant DATABASE_RELOAD. */
	public static final String DATABASE_RELOAD="databaseReload"; //$NON-NLS-1$
	
	/** The Constant SHOW_HOVER. */
	public static final String SHOW_HOVER="showWaveformHover"; //$NON-NLS-1$
	
	/** The Constant SHOW_TX_DETAILS. */
	public static final String SHOW_TX_DETAILS="showTxDetails"; //$NON-NLS-1$
	
	/** The Constant TRACK_HEIGHT. */
	public static final String TRACK_HEIGHT="trackHeigth"; //$NON-NLS-1$
	
	private PreferenceConstants() {}
}
