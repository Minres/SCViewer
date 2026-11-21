/*******************************************************************************
 * Copyright (c) 2020-2022 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
 
package com.minres.scviewer.e4.application.handlers;

import java.util.LinkedList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.prefs.BackingStoreException;

import com.minres.scviewer.e4.application.preferences.PreferenceConstants;

public class EnableHover {
	static final String TAG_NAME = "EnableHover"; //$NON-NLS-1$

	@Inject
	MApplication application;
		
	@Inject
	@Optional
	public void reactOnShowHoverChange(EModelService modelService, @Preference(value = PreferenceConstants.SHOW_HOVER) Boolean hover) {
		List<String> tags = new LinkedList<>();
		tags.add(TAG_NAME);
		List<MHandledItem> elements = modelService.findElements(application, null, MHandledItem.class, tags );
		for( MHandledItem hi : elements ){
			hi.setSelected(hover);
		}
	}

	@Execute
	public void execute(MHandledItem handledItem, @Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE) IEclipsePreferences prefs ) {
		try {
			prefs.putBoolean(PreferenceConstants.SHOW_HOVER, handledItem.isSelected());
			prefs.flush();
		} catch (BackingStoreException e) {}
	}
		
}