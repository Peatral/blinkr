package xyz.peatral.blinkr.feature.glyph.domain

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
import xyz.peatral.blinkr.feature.glyph.data.GlyphRenderCommand
import xyz.peatral.blinkr.feature.glyph.data.GlyphRepository
import xyz.peatral.blinkr.feature.glyph.data.GlyphSettingsRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class UpdateGlyphDisplayUseCase @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val glyphSettingsRepository: GlyphSettingsRepository,
    private val glyphRepository: GlyphRepository,
    private val timerRepository: TimerRepository,
    private val flashGlyph: FlashGlyphUseCase,
) {
    companion object {
        private const val TIMER_LAYER_ID = "timer_display_layer"
        private const val TIMER_PRIORITY = 50
    }

    suspend operator fun invoke(mode: GlyphMode = GlyphMode.APP) = coroutineScope {
        var flashJob: Job? = null

        try {
            timerRepository.timerState.collectLatest { state ->
                when (state) {
                    is TimerState.Idle -> {
                        flashJob?.cancelAndJoin()

                        if (mode == GlyphMode.TOY) {
                            val settings = glyphSettingsRepository.settings.first()

                            glyphRepository.updateLayer(
                                mode = mode,
                                layerId = TIMER_LAYER_ID,
                                priority = TIMER_PRIORITY,
                                command = GlyphRenderCommand.Text(
                                    text = "RDY",
                                    brightness = settings.timerBrightness
                                )
                            )
                        } else {
                            glyphRepository.removeLayer(mode, TIMER_LAYER_ID)
                        }
                    }

                    is TimerState.Expired -> {
                        val settings = glyphSettingsRepository.settings.first()

                        if ((mode == GlyphMode.APP && !settings.isEnabled) || settings.flashDurationSeconds <= 0) {
                            glyphRepository.removeLayer(mode, TIMER_LAYER_ID)
                            return@collectLatest
                        }

                        flashJob = launch {
                            flashGlyph(
                                duration = settings.flashDurationSeconds.seconds,
                                brightness = settings.flashBrightness,
                                mode = mode,
                            )
                            glyphRepository.removeLayer(mode, TIMER_LAYER_ID)
                        }
                    }

                    is TimerState.Running -> {
                        streamCountdownToGlyph(mode)
                    }
                }
            }
        } finally {
            glyphRepository.removeLayer(mode, TIMER_LAYER_ID)
        }
    }

    private suspend fun streamCountdownToGlyph(mode: GlyphMode) {
        combine(
            formatTimerUseCase(),
            glyphSettingsRepository.settings
        ) { time, settings -> time to settings }
            .collect { (time, settings) ->
                if ((mode == GlyphMode.APP && !settings.isEnabled) || time.isBlank()) {
                    glyphRepository.removeLayer(mode, TIMER_LAYER_ID)
                } else {
                    glyphRepository.updateLayer(
                        mode = mode,
                        layerId = TIMER_LAYER_ID,
                        priority = TIMER_PRIORITY,
                        command = GlyphRenderCommand.Text(
                            text = time,
                            brightness = settings.timerBrightness
                        )
                    )
                }
            }
    }
}