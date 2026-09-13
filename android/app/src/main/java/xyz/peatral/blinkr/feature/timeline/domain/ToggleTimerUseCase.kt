package xyz.peatral.blinkr.feature.timeline.domain

import kotlinx.coroutines.flow.first
import xyz.peatral.blinkr.core.data.repository.SessionSettingsRepository
import xyz.peatral.blinkr.core.data.repository.Timer
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ToggleTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionSettingsRepository: SessionSettingsRepository,
) {
    suspend operator fun invoke() {
        val currentState = timerRepository.timerState.first()
        val now = Clock.System.now()
        val intervalMins = sessionSettingsRepository.intervalMins.first()

        when (currentState) {
            is TimerState.Idle -> {
                timerRepository.updateState(TimerState.Running(Timer(now, now + intervalMins.minutes)))
            }
            else -> {
                timerRepository.updateState(TimerState.Idle())
            }
        }
    }
}
