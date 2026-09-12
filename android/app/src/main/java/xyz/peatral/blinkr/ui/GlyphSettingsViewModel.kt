package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.repository.GlyphSettings
import xyz.peatral.blinkr.data.repository.GlyphSettingsRepository
import javax.inject.Inject

@HiltViewModel
class GlyphSettingsViewModel @Inject constructor(
    private val glyphSettingsRepository: GlyphSettingsRepository,
) : ViewModel() {

    val settings = glyphSettingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlyphSettings(
            isEnabled = GlyphSettingsRepository.DEFAULT_IS_ENABLED,
            brightness = GlyphSettingsRepository.DEFAULT_BRIGHTNESS,
        )
    )

    fun setIsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            glyphSettingsRepository.setIsEnabled(enabled)
        }
    }

    fun setBrightness(brightness: Int) {
        viewModelScope.launch {
            glyphSettingsRepository.setBrightness(brightness)
        }
    }
}
