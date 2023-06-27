/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.resource.StringConverter;
import org.osgi.framework.FrameworkUtil;

import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.database.ui.swt.DefaultWaveformStyleProvider;

/**
 * The Class DefaultValuesInitializer.
 */
public class DefaultValuesInitializer extends AbstractPreferenceInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences store = DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		DefaultWaveformStyleProvider styleProvider = new DefaultWaveformStyleProvider();
		
		store.putBoolean(PreferenceConstants.DATABASE_RELOAD, true);
		store.putBoolean(PreferenceConstants.SHOW_HOVER, true);
		store.putBoolean(PreferenceConstants.SHOW_TX_DETAILS, false);
		store.putInt(PreferenceConstants.TRACK_HEIGHT, 30);
        for (WaveformColors c : WaveformColors.values()) {
        	 store.put(c.name()+"_COLOR", StringConverter.asString(styleProvider.getColor(c).getRGB())); //$NON-NLS-1$
        }
	}

}
