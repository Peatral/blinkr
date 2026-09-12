package xyz.peatral.blinkr.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class GlyphSettings(
    val isEnabled: Boolean,
    val brightness: Int,
)

@Singleton
class GlyphSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val IS_ENABLED_KEY = booleanPreferencesKey("is_glyph_enabled")
        val BRIGHTNESS_KEY = intPreferencesKey("glyph_brightness")

        const val DEFAULT_IS_ENABLED = true
        const val DEFAULT_BRIGHTNESS = 128
    }

    val settings: Flow<GlyphSettings> = context.dataStore.data.map { preferences ->
        GlyphSettings(
            isEnabled = preferences[IS_ENABLED_KEY] ?: DEFAULT_IS_ENABLED,
            brightness = preferences[BRIGHTNESS_KEY] ?: DEFAULT_BRIGHTNESS,
        )
    }

    suspend fun setIsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setBrightness(brightness: Int) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_KEY] = brightness
        }
    }
}
