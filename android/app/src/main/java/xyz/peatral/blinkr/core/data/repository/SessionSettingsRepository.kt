package xyz.peatral.blinkr.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.peatral.blinkr.core.di.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

// TODO: this just mirrors pebble - not sure what the best way to merge these two is
// Maybe pebble just updates this and move the settings to a general category?
@Singleton
class SessionSettingsRepository @Inject constructor(
    @SettingsDataStore private val settingsDataStore: DataStore<Preferences>,
) {
    companion object {
        val INTERVAL_MINS_KEY = intPreferencesKey("interval_mins")
        const val DEFAULT_INTERVAL_MINS = 20
    }

    val intervalMins: Flow<Int> = settingsDataStore.data.map { preferences ->
        preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
    }
}
