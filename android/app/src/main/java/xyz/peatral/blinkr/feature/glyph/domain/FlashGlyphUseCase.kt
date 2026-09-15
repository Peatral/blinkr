package xyz.peatral.blinkr.feature.glyph.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import xyz.peatral.blinkr.feature.glyph.data.GlyphMode
import xyz.peatral.blinkr.feature.glyph.data.GlyphRenderCommand
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FlashGlyphUseCase @Inject constructor(
    private val glyphRepository: GlyphRepository
) {
    companion object {
        private val FLASH_INTERVAL = 100.milliseconds
        private const val LAYER_ID = "flash_layer"
        private const val PRIORITY = 100
    }

    suspend operator fun invoke(duration: Duration, brightness: Int, mode: GlyphMode = GlyphMode.APP) {
        if (!duration.isPositive()) return

        try {
            withTimeoutOrNull(duration) {
                var isOn = true
                while (isActive) {
                    val command = if (isOn) {
                        GlyphRenderCommand.AllOn(brightness)
                    } else {
                        GlyphRenderCommand.Clear
                    }

                    glyphRepository.updateLayer(mode, LAYER_ID, PRIORITY, command)

                    isOn = !isOn
                    delay(FLASH_INTERVAL)
                }
            }
        } finally {
            glyphRepository.removeLayer(mode, LAYER_ID)
        }
    }
}