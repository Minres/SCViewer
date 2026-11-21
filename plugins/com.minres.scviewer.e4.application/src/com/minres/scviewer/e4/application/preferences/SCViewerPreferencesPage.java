/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;

import com.minres.scviewer.e4.application.Messages;


/**
 * The Class SCViewerPreferencesPage showing the SCViewer top preferences.
 */
public class SCViewerPreferencesPage extends FieldEditorPreferencePage {

	/**
	 * Instantiates a new SC viewer preferences page.
	 */
	public SCViewerPreferencesPage() {
		super(GRID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {

		addField(new BooleanFieldEditor(PreferenceConstants.DATABASE_RELOAD, Messages.SCViewerPreferencesPage_0,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.SHOW_HOVER, Messages.SCViewerPreferencesPage_1,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.SHOW_TX_DETAILS, Messages.SCViewerPreferencesPage_3,
				getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.TRACK_HEIGHT, Messages.SCViewerPreferencesPage_2,
				getFieldEditorParent()));

	}

}
