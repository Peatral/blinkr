package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.flow.first
import xyz.peatral.blinkr.core.data.repository.SessionSettingsRepository
import xyz.peatral.blinkr.core.data.repository.Timer
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class RescheduleTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionSettingsRepository: SessionSettingsRepository,
) {
    suspend operator fun invoke() {
        timerRepository.timerState.collect { timerState ->
            if (timerState is TimerState.Expired) {
                val now = Clock.System.now()
                val intervalMins = sessionSettingsRepository.intervalMins.first()
                val interval = intervalMins.minutes

                val startTime = timerState.timer.start
                val elapsedTime = now - startTime
                val periods = elapsedTime.inWholeMilliseconds / interval.inWholeMilliseconds
                val endTime = startTime + interval * (periods + 1).toInt()

                timerRepository.updateState(TimerState.Running(Timer(startTime, endTime)))
            }
        }
    }
}
