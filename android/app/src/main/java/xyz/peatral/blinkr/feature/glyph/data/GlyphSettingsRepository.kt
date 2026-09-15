package xyz.peatral.blinkr.feature.glyph.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.peatral.blinkr.core.di.ApplicationScope
import xyz.peatral.blinkr.core.di.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

data class GlyphSettings(
    val isEnabled: Boolean,
    val timerBrightness: Int,
    val flashDurationSeconds: Int,
    val flashBrightness: Int,
)

@Singleton
class GlyphSettingsRepository @Inject constructor(
    @SettingsDataStore private val settingsDataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    companion object {
        val IS_ENABLED_KEY = booleanPreferencesKey("is_glyph_enabled")
        val TIMER_BRIGHTNESS_KEY = intPreferencesKey("glyph_timer_brightness")
        val FLASH_DURATION_KEY = intPreferencesKey("glyph_flash_duration")
        val FLASH_BRIGHTNESS_KEY = intPreferencesKey("glyph_flash_brightness")

        const val DEFAULT_IS_ENABLED = true
        const val DEFAULT_BRIGHTNESS = 128
        const val DEFAULT_FLASH_DURATION = 5
        const val DEFAULT_FLASH_BRIGHTNESS = 256
    }

    val settings: StateFlow<GlyphSettings> = settingsDataStore.data.map { preferences ->
        GlyphSettings(
            isEnabled = preferences[IS_ENABLED_KEY] ?: DEFAULT_IS_ENABLED,
            timerBrightness = preferences[TIMER_BRIGHTNESS_KEY] ?: DEFAULT_BRIGHTNESS,
            flashDurationSeconds = preferences[FLASH_DURATION_KEY] ?: DEFAULT_FLASH_DURATION,
            flashBrightness = preferences[FLASH_BRIGHTNESS_KEY] ?: DEFAULT_FLASH_BRIGHTNESS,
        )
    }.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = GlyphSettings(
            isEnabled = DEFAULT_IS_ENABLED,
            timerBrightness = DEFAULT_BRIGHTNESS,
            flashDurationSeconds = DEFAULT_FLASH_DURATION,
            flashBrightness = DEFAULT_FLASH_BRIGHTNESS,
        )
    )

    suspend fun setIsEnabled(enabled: Boolean) {
        settingsDataStore.edit { preferences ->
            preferences[IS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setBrightness(brightness: Int) {
        settingsDataStore.edit { preferences ->
            preferences[TIMER_BRIGHTNESS_KEY] = brightness
        }
    }

    suspend fun setFlashDuration(seconds: Int) {
        settingsDataStore.edit { preferences ->
            preferences[FLASH_DURATION_KEY] = seconds
        }
    }

    suspend fun setFlashBrightness(brightness: Int) {
        settingsDataStore.edit { preferences ->
            preferences[FLASH_BRIGHTNESS_KEY] = brightness
        }
    }
}
