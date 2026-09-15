package xyz.peatral.blinkr.feature.glyph.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettings
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettingsRepository
import xyz.peatral.blinkr.feature.glyph.domain.FlashGlyphUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class GlyphSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glyphSettingsRepository: GlyphSettingsRepository,
    private val flashGlyph: FlashGlyphUseCase
) : ViewModel() {
    private var testFlashJob: Job? = null

    val settings = glyphSettingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlyphSettings(
            isEnabled = GlyphSettingsRepository.DEFAULT_IS_ENABLED,
            timerBrightness = GlyphSettingsRepository.DEFAULT_BRIGHTNESS,
            flashDurationSeconds = GlyphSettingsRepository.DEFAULT_FLASH_DURATION,
            flashBrightness = GlyphSettingsRepository.DEFAULT_FLASH_BRIGHTNESS,
        )
    )

    fun setIsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            glyphSettingsRepository.setIsEnabled(enabled)
        }
    }

    fun setTimerBrightness(brightness: Int) {
        viewModelScope.launch {
            glyphSettingsRepository.setBrightness(brightness)
        }
    }

    fun setFlashDuration(seconds: Int) {
        viewModelScope.launch {
            glyphSettingsRepository.setFlashDuration(seconds)
        }
    }

    fun setFlashBrightness(brightness: Int) {
        viewModelScope.launch {
            glyphSettingsRepository.setFlashBrightness(brightness)
        }
    }


    fun testFlash() {
        testFlashJob?.cancel()
        testFlashJob = viewModelScope.launch {
            flashGlyph(
                duration = glyphSettingsRepository.settings.value.flashDurationSeconds.seconds,
                brightness = glyphSettingsRepository.settings.value.flashBrightness
            )
        }
    }

    fun openManageGlyphToys() {
        val intent = Intent().apply {
            component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
        }
        context.startActivity(intent)
    }
}
