 
package com.minres.scviewer.e4.application.handlers;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import com.minres.scviewer.e4.application.preferences.PreferenceConstants;

@SuppressWarnings("restriction")
public class EnableHover {
	static final String TAG_NAME = "EnableHover"; //$NON-NLS-1$

	@Inject
	@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE)
	IEclipsePreferences prefs;

	@Inject
	MApplication application;
		
	@PostConstruct
	public void initialize(EModelService modelService) {
		List<String> tags = new LinkedList<>();
		tags.add(TAG_NAME);
		List<MHandledItem> elements = modelService.findElements(application, null, MHandledItem.class, tags );
	   // cover initialization stuff, sync it with code
		for( MHandledItem hi : elements ){
			hi.setSelected(prefs.getBoolean(PreferenceConstants.SHOW_HOVER, true));
		}
	}

	@Execute
	public void execute(@Active MPart part, @Active MWindow window, MHandledItem handledItem, EModelService modelService ) {
		prefs.putBoolean(PreferenceConstants.SHOW_HOVER, handledItem.isSelected());
	}
		
}