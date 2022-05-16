
package com.minres.scviewer.e4.application.handlers;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.osgi.service.prefs.BackingStoreException;

import com.minres.scviewer.e4.application.preferences.PreferenceConstants;

public class EnableTxDetails {
	static final String TAG_NAME = "EnableTxDetails"; //$NON-NLS-1$

	static final String ICON_DISABLED = "platform:/plugin/com.minres.scviewer.e4.application/icons/application_side_expand.png"; //$NON-NLS-1$
	static final String ICON_ENABLED = "platform:/plugin/com.minres.scviewer.e4.application/icons/application_side_contract.png"; //$NON-NLS-1$
	static final String TOOLTIP_DISABLED = "Show tx details parts";
	static final String TOOLTIP_ENABLED = "Hide tx details parts";
	
	@Inject
	MApplication application;

	@Inject
	EPartService partService;

	@Inject
	@Optional
	public void reactOnShowHoverChange(EModelService modelService, @Preference(value = PreferenceConstants.SHOW_TX_DETAILS) Boolean show) {
		List<String> tags = new LinkedList<>();
		tags.add(TAG_NAME);
		List<MHandledItem> elements = modelService.findElements(application, null, MHandledItem.class, tags );
		for( MHandledItem hi : elements ){
			hi.setSelected(show);
			hi.setIconURI(show?ICON_ENABLED:ICON_DISABLED);
			hi.setTooltip(show?TOOLTIP_ENABLED:TOOLTIP_DISABLED);
		}
	}

	@Execute
	public void execute(MHandledItem handledItem, @Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE) IEclipsePreferences prefs ) {
		try {
			prefs.putBoolean(PreferenceConstants.SHOW_TX_DETAILS, handledItem.isSelected());
			prefs.flush();
		} catch (BackingStoreException e) {}
	}

}