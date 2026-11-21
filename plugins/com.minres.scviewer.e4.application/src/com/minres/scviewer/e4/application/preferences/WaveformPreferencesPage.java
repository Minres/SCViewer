/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.preferences;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.osgi.util.NLS;

import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.e4.application.Messages;

/**
 *  The WaveformView preference page to show the colors to use.
 */
public class WaveformPreferencesPage extends FieldEditorPreferencePage {

	/**
	 * Instantiates a new waveform preferences page.
	 */
	public WaveformPreferencesPage() {
		super(GRID);
		setDescription(Messages.WaveformPreferencesPage_description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		Field[] declaredFields = Messages.class.getDeclaredFields();
		HashMap<String, String> staticFields = new HashMap<String, String>();
		for (Field field : declaredFields) {
		    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
		        try {
					staticFields.put(field.getName(), (String)field.get(null));
				} catch (IllegalArgumentException | IllegalAccessException e) {}
		    }
		}
		for (WaveformColors c : WaveformColors.values()) {
			addField(new ColorFieldEditor(c.name() + "_COLOR", 
					NLS.bind(Messages.WaveformPreferencesPage_1, staticFields.get(c.name().toLowerCase())), //$NON-NLS-1$
					getFieldEditorParent()));
		}
	}

}
