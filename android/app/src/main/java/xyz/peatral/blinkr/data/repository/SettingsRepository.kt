package xyz.peatral.blinkr.data.repository

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
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.datasource.pebble.PebbleMessage
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val intervalMins: Int,
)

@Singleton
class SettingsRepository @Inject constructor(
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

    val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            intervalMins = preferences[INTERVAL_MINS_KEY] ?: DEFAULT_INTERVAL_MINS
        )
    }

    suspend fun setIntervalMins(intervalMins: Int) {
        context.dataStore.edit { preferences ->
            preferences[INTERVAL_MINS_KEY] = intervalMins
        }
    }
}
