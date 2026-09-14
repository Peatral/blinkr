package xyz.peatral.blinkr.feature.timer.domain

import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SyncRepository
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PersistSessionToStorageUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke() {
        timerRepository.timerState.collect { state ->
            when (state) {
                is TimerState.Idle -> {
                    val startTime = syncRepository.getUnfinishedSessionStartTime() ?: return@collect
                    val endTime = state.timestamp ?: return@collect
                    if (isValidSession(startTime, endTime)) {
                        syncRepository.saveSession(
                            SessionEntity(
                                startTime,
                                endTime,
                            )
                        )
                    } else {
                        syncRepository.deleteUnfinishedSession()
                    }
                }
                is TimerState.Running -> {
                    val start = syncRepository.getUnfinishedSessionStartTime()
                    if (start == null) {
                        syncRepository.saveSession(
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