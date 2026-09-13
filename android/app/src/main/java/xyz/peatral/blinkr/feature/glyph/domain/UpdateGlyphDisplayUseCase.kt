package xyz.peatral.blinkr.feature.glyph.domain

import com.nothing.ketchum.Common
import kotlinx.coroutines.flow.combine
import xyz.peatral.blinkr.core.domain.FormatTimerUseCase
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettingsRepository
import javax.inject.Inject

class UpdateGlyphDisplayUseCase @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val glyphSettingsRepository: GlyphSettingsRepository,
    private val glyphRepository: GlyphRepository,
) {
    suspend operator fun invoke() {
        try {
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
        }
    }
}