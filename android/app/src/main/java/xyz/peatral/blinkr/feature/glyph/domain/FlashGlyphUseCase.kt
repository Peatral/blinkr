package xyz.peatral.blinkr.feature.glyph.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val FLASH_INTERVAL = 100.milliseconds

class FlashGlyphUseCase @Inject constructor(
    private val glyphRepository: GlyphRepository
) {
    suspend operator fun invoke(durationSeconds: Int, brightness: Int) {
        if (durationSeconds <= 0) return

        try {
            glyphRepository.connect()

            glyphRepository.withHardwareLock {
                withTimeoutOrNull(durationSeconds.seconds) {
                    var isOn = true
                    while (isActive) {
                        if (isOn) {
                            glyphRepository.turnOnAll(brightness)
                        } else {
                            glyphRepository.clearDisplay()
                        }
                        isOn = !isOn
                        delay(FLASH_INTERVAL)
                    }
                }
                glyphRepository.clearDisplay()
            }
        } finally {
            glyphRepository.disconnect()
        }
    }
}