package xyz.peatral.blinkr.feature.glyph.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import xyz.peatral.blinkr.feature.glyph.data.GlyphMode
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val FLASH_INTERVAL = 100.milliseconds

class FlashGlyphUseCase @Inject constructor(
    private val glyphRepository: GlyphRepository
) {
    suspend operator fun invoke(durationSeconds: Int, brightness: Int, mode: GlyphMode = GlyphMode.APP) {
        if (durationSeconds <= 0) return

        try {
            glyphRepository.connect(mode)

            glyphRepository.withHardwareLock {
                withTimeoutOrNull(durationSeconds.seconds) {
                    var isOn = true
                    while (isActive) {
                        if (isOn) {
                            glyphRepository.turnOnAll(brightness, mode)
                        } else {
                            glyphRepository.clearDisplay(mode)
                        }
                        isOn = !isOn
                        delay(FLASH_INTERVAL)
                    }
                }
                glyphRepository.clearDisplay(mode)
            }
        } finally {
            glyphRepository.disconnect(mode)
        }
    }
}