package xyz.peatral.blinkr.feature.glyph.domain

import com.nothing.ketchum.Common
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
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
    private val timerRepository: TimerRepository,
    private val wakeLockRepository: WakeLockRepository,
    private val flashGlyphUseCase: FlashGlyphUseCase,
) {
    companion object {
        const val WAKELOCK_TAG = "Blinkr:GlyphTimerWakeLock"
        val WAKELOCK_DURATION = 10.hours
    }

    suspend operator fun invoke() = coroutineScope {
        var flashJob: Job? = null

        try {
            wakeLockRepository.acquire(WAKELOCK_TAG, WAKELOCK_DURATION)
            glyphRepository.connect()

            timerRepository.timerState.collectLatest { state ->
                when (state) {
                    is TimerState.Idle -> {
                        flashJob?.cancel()
                        glyphRepository.tryHardwareLock { glyphRepository.clearDisplay() }
                    }

                    is TimerState.Expired -> {
                        val settings = glyphSettingsRepository.settings.first()

                        if (!settings.isEnabled || settings.flashDurationSeconds <= 0) {
                            timerRepository.compareAndSetState(state, TimerState.Idle)
                            return@collectLatest
                        }

                        flashJob = launch {
                            flashGlyphUseCase(
                                durationSeconds = settings.flashDurationSeconds,
                                brightness = settings.flashBrightness
                            )
                            timerRepository.compareAndSetState(state, TimerState.Idle)
                        }
                    }

                    is TimerState.Running -> {
                        streamCountdownToGlyph()
                    }
                }
            }
        } finally {
            glyphRepository.clearDisplay()
            glyphRepository.disconnect()
            wakeLockRepository.release()
        }
    }

    private suspend fun streamCountdownToGlyph() {
        combine(
            formatTimerUseCase(),
            glyphSettingsRepository.settings
        ) { time, settings -> time to settings }
            .collect { (time, settings) ->
                glyphRepository.tryHardwareLock {
                    if (!settings.isEnabled || time.isBlank()) {
                        glyphRepository.clearDisplay()
                    } else {
                        val approxTextHeight = 5
                        val approxTextWidth = 4 * 4 + 4 + 1 // 4 numbers a 4 px, 4 paddings, the colon
                        drawCenteredText(
                            text = time,
                            brightness = settings.timerBrightness,
                            width = approxTextWidth,
                            height = approxTextHeight,
                        )
                    }
                }
            }
    }

    private fun drawCenteredText(text: String, brightness: Int, width: Int, height: Int) {
        val matrixSize = Common.getDeviceMatrixLength()

        glyphRepository.displayText(
            text = text,
            x = (matrixSize - width) / 2,
            y = (matrixSize - height) / 2,
            brightness = brightness
        )
    }
}