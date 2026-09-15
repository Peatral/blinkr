package xyz.peatral.blinkr.feature.glyph.domain

import com.nothing.ketchum.Common
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.core.domain.FormatTimerUseCase
import xyz.peatral.blinkr.feature.glyph.data.GlyphMode
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettingsRepository
import xyz.peatral.blinkr.feature.glyph.data.WakeLockRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class UpdateGlyphDisplayUseCase @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val glyphSettingsRepository: GlyphSettingsRepository,
    private val glyphRepository: GlyphRepository,
    private val timerRepository: TimerRepository,
    private val wakeLockRepository: WakeLockRepository,
    private val flashGlyphUseCase: FlashGlyphUseCase,
) {
    companion object {
        const val WAKELOCK_TAG = "Blinkr:GlyphTimerWakeLock"
        val WAKELOCK_DURATION = 10.hours
    }

    suspend operator fun invoke(mode: GlyphMode = GlyphMode.APP) = coroutineScope {
        var flashJob: Job? = null

        try {
            wakeLockRepository.acquire(WAKELOCK_TAG, WAKELOCK_DURATION)
            glyphRepository.connect(mode)

            timerRepository.timerState.collectLatest { state ->
                when (state) {
                    is TimerState.Idle -> {
                        flashJob?.cancelAndJoin()

                        if (mode == GlyphMode.TOY) {
                            val settings = glyphSettingsRepository.settings.first()
                            glyphRepository.tryHardwareLock {
                                val approxTextHeight = 5
                                val approxTextWidth = 3 * 4 + 2 // 3 letters a 4 pixels + 2 paddings
                                drawCenteredText(
                                    text = "RDY",
                                    brightness = settings.timerBrightness,
                                    width = approxTextWidth,
                                    height = approxTextHeight,
                                    mode = mode,
                                )
                            }
                        } else {
                            clearDisplaySafe(mode)
                        }
                    }

                    is TimerState.Expired -> {
                        val settings = glyphSettingsRepository.settings.first()

                        if ((mode == GlyphMode.APP && !settings.isEnabled) || settings.flashDurationSeconds <= 0) {
                            clearDisplaySafe(mode)
                            return@collectLatest
                        }

                        flashJob = launch {
                            flashGlyphUseCase(
                                durationSeconds = settings.flashDurationSeconds,
                                brightness = settings.flashBrightness,
                                mode = mode,
                            )
                            clearDisplaySafe(mode)
                        }
                    }

                    is TimerState.Running -> {
                        streamCountdownToGlyph(mode)
                    }
                }
            }
        } finally {
            glyphRepository.tryHardwareLock { glyphRepository.clearDisplay(mode) }
            glyphRepository.disconnect(mode)
            wakeLockRepository.release()
        }
    }

    private fun clearDisplaySafe(mode: GlyphMode) {
        glyphRepository.tryHardwareLock { glyphRepository.clearDisplay(mode) }
    }

    private suspend fun streamCountdownToGlyph(mode: GlyphMode) {
        combine(
            formatTimerUseCase(),
            glyphSettingsRepository.settings
        ) { time, settings -> time to settings }
            .collect { (time, settings) ->
                glyphRepository.tryHardwareLock {
                    if ((mode == GlyphMode.APP && !settings.isEnabled) || time.isBlank()) {
                        glyphRepository.clearDisplay(mode)
                    } else {
                        val approxTextHeight = 5
                        val approxTextWidth = 4 * 4 + 4 + 1 // 4 numbers a 4 px, 4 paddings, the colon
                        drawCenteredText(
                            text = time,
                            brightness = settings.timerBrightness,
                            width = approxTextWidth,
                            height = approxTextHeight,
                            mode = mode,
                        )
                    }
                }
            }
    }

    private fun drawCenteredText(text: String, brightness: Int, width: Int, height: Int, mode: GlyphMode) {
        val matrixSize = Common.getDeviceMatrixLength()

        glyphRepository.displayText(
            text = text,
            x = (matrixSize - width) / 2,
            y = (matrixSize - height) / 2,
            brightness = brightness,
            mode = mode
        )
    }
}