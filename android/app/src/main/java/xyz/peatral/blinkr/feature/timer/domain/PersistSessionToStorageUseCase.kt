package xyz.peatral.blinkr.feature.timer.domain

import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PersistSessionToStorageUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        timerRepository.timerState.collect { state ->
            when (state) {
                is TimerState.Idle -> {
                    val startTime = sessionRepository.getUnfinishedSessionStartTime() ?: return@collect
                    val endTime = state.timestamp ?: return@collect
                    if (isValidSession(startTime, endTime)) {
                        sessionRepository.saveSession(
                            SessionEntity(
                                startTime,
                                endTime,
                            )
                        )
                    } else {
                        sessionRepository.deleteUnfinishedSession()
                    }
                }
                is TimerState.Running -> {
                    val start = sessionRepository.getUnfinishedSessionStartTime()
                    if (start == null) {
                        sessionRepository.saveSession(
                            SessionEntity(
                                startTime = state.timer.start,
                                endTime = null
                            )
                        )
                    }
                }
                is TimerState.Expired -> {}
            }
        }
    }

    private fun isValidSession(start: Instant, end: Instant): Boolean {
        return start < end &&
                end - start >= 1.minutes
    }
}