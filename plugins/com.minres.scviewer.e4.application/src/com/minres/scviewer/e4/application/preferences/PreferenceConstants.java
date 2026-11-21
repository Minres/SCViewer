/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
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
