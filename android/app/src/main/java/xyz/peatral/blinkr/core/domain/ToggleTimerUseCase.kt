package xyz.peatral.blinkr.core.domain

import kotlinx.coroutines.flow.first
import xyz.peatral.blinkr.core.data.repository.SessionRepository
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
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        val currentState = timerRepository.timerState.first()
        val now = Clock.System.now()
        val intervalMins = sessionSettingsRepository.intervalMins.first()

        when (currentState) {
            is TimerState.Idle -> {
                var startTime = now
                val latestSession = sessionRepository.getLatestSession()

                if (latestSession != null) {
                    if (latestSession.isActive) {
                        startTime = latestSession.startTime
                    } else if (latestSession.endTime != null) {
                        val diff = now - latestSession.endTime
                        if (diff.isPositive() && diff < 1.minutes) {
                            startTime = latestSession.startTime
                        }
                    }
                }

                val interval = intervalMins.minutes
                val elapsedTime = now - startTime
                val periods = elapsedTime.inWholeMilliseconds / interval.inWholeMilliseconds
                val endTime = startTime + interval * (periods + 1).toInt()

                timerRepository.updateState(TimerState.Running(Timer(startTime, endTime)))
            }
            else -> {
                timerRepository.updateState(TimerState.Idle())
            }
        }
    }
}
