package org.odk.collect.android.application.initialization;

import org.odk.collect.android.preferences.source.PreferencesDataSource;

public interface SettingsPreferenceMigrator {

    void migrate(PreferencesDataSource generalPreferences, PreferencesDataSource adminPreferences);
}
