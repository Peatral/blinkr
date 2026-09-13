package xyz.peatral.blinkr.feature.pebble.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.settingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

data class PebbleSettings(
    val intervalMins: Int,
)

@Singleton
class PebbleSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pebbleRepository: PebbleRepository,
) {
    companion object {
        val INTERVAL_MINS_KEY = intPreferencesKey("interval_mins")
        const val DEFAULT_INTERVAL_MINS = 20
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        repoScope.launch {
            pebbleRepository.incomingMessages.collect { message ->
                if (message is PebbleMessage.UpdateSettings) {
                    setIntervalMins(message.intervalMins)
                }
            }
        }
    }

    val settings: Flow<PebbleSettings> = context.settingsDataStore.data.map { preferences ->
        PebbleSettings(
            intervalMins = preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
        )
    }

    suspend fun setIntervalMins(intervalMins: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[INTERVAL_MINS_KEY] = intervalMins
        }
    }
}
