package xyz.peatral.blinkr.feature.pebble.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import xyz.peatral.blinkr.core.di.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

data class PebbleSettings(
    val intervalMins: Int,
)

@Singleton
class PebbleSettingsRepository @Inject constructor(
    @SettingsDataStore private val settingsDataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
    private val pebbleRepository: PebbleRepository,
) {
    companion object {
        val INTERVAL_MINS_KEY = intPreferencesKey("interval_mins")
        const val DEFAULT_INTERVAL_MINS = 20
    }

    init {
        appScope.launch {
            pebbleRepository.incomingMessages.collect { message ->
                if (message is PebbleMessage.UpdateSettings) {
                    setIntervalMins(message.intervalMins)
                }
            }
        }
    }

    val settings: Flow<PebbleSettings> = settingsDataStore.data.map { preferences ->
        PebbleSettings(
            intervalMins = preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
        )
    }

    suspend fun setIntervalMins(intervalMins: Int) {
        settingsDataStore.edit { preferences ->
            preferences[INTERVAL_MINS_KEY] = intervalMins
        }
    }
}
