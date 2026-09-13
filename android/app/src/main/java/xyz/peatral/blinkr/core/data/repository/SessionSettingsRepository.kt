package xyz.peatral.blinkr.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.peatral.blinkr.core.data.settingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

// TODO: this just mirrors pebble - not sure what the best way to merge these two is
// Maybe pebble just updates this and move the settings to a general category?
@Singleton
class SessionSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val INTERVAL_MINS_KEY = intPreferencesKey("interval_mins")
        const val DEFAULT_INTERVAL_MINS = 20
    }

    val intervalMins: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
    }
}
