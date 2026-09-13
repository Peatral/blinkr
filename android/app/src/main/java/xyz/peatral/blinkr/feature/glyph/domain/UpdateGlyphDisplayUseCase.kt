package xyz.peatral.blinkr.feature.glyph.domain

import com.nothing.ketchum.Common
import kotlinx.coroutines.flow.combine
import xyz.peatral.blinkr.core.domain.FormatTimerUseCase
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettingsRepository
import xyz.peatral.blinkr.feature.glyph.data.WakeLockRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class UpdateGlyphDisplayUseCase @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val glyphSettingsRepository: GlyphSettingsRepository,
    private val glyphRepository: GlyphRepository,
    private val wakeLockRepository: WakeLockRepository,
) {
    companion object {
        const val WAKELOCK_TAG = "Blinkr:GlyphTimerWakeLock"
        val WAKELOCK_DURATION = 10.hours
    }

    suspend operator fun invoke() {
        try {
            wakeLockRepository.acquire(WAKELOCK_TAG, WAKELOCK_DURATION)
            glyphRepository.connect()

            combine(
                formatTimerUseCase(),
                glyphSettingsRepository.settings
            ) { formattedTime, settings ->
                formattedTime to settings
            }.collect { (formattedTime, settings) ->
                if (!settings.isEnabled || formattedTime.isBlank()) {
                    glyphRepository.clearDisplay()
                } else {
                    val matrixSize = Common.getDeviceMatrixLength()

                    val approxTextHeight = 5
                    val approxTextWidth = 4 * 4 + 4 + 1 // 4 numbers a 4 px, 4 paddings, the colon

                    val centerY = (matrixSize - approxTextHeight) / 2
                    val centerX = (matrixSize - approxTextWidth) / 2

                    glyphRepository.displayText(
                        text = formattedTime,
                        x = centerX,
                        y = centerY,
                        brightness = settings.brightness,
                    )
                }
            }
        } finally {
            glyphRepository.clearDisplay()
            glyphRepository.disconnect()
            wakeLockRepository.release()
        }
    }
}