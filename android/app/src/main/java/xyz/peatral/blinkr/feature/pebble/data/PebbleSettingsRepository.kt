package xyz.peatral.blinkr.feature.pebble.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.core.data.datasource.pebble.PebbleMessage
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class PebbleSettings(
    val intervalMins: Int,
)

@Singleton
class PebbleSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pebbleDataSource: PebbleDataSource,
) {
    companion object {
        val INTERVAL_MINS_KEY = intPreferencesKey("interval_mins")
        const val DEFAULT_INTERVAL_MINS = 20
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        repoScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                if (message is PebbleMessage.UpdateSettings) {
                    setIntervalMins(message.intervalMins)
                }
            }
        }
    }

    val settings: Flow<PebbleSettings> = context.dataStore.data.map { preferences ->
        PebbleSettings(
            intervalMins = preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
        )
    }

    suspend fun setIntervalMins(intervalMins: Int) {
        context.dataStore.edit { preferences ->
            preferences[INTERVAL_MINS_KEY] = intervalMins
        }
    }
}
