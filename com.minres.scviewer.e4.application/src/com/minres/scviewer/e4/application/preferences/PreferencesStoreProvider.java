package com.minres.scviewer.e4.application.preferences;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;

import com.opcoach.e4.preferences.IPreferenceStoreProvider;
import com.opcoach.e4.preferences.ScopedPreferenceStore;

public class PreferencesStoreProvider implements IPreferenceStoreProvider{

	public PreferencesStoreProvider(){
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(ConfigurationScope.INSTANCE, PreferenceConstants.PREFERENCES_SCOPE);
	}

}