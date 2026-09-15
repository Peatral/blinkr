package xyz.peatral.blinkr.core.domain

import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ReconcileSessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(currentTime: Instant) {
        val history = sessionRepository.getAllSessionsAsc()
        if (history.isEmpty()) return

        val activeSession = history.lastOrNull { it.isActive }
        val completedSessions = history.filter { !it.isActive }
        val maxAllowedTime = currentTime + 1.days

        val validHistory = completedSessions.filter {
            it.startTime.toEpochMilliseconds() > 0 &&
                    it.endTime!! > it.startTime &&
                    it.endTime <= maxAllowedTime
        }

        val mergedHistory = mutableListOf<SessionEntity>()
        for (current in validHistory) {
            val last = mergedHistory.lastOrNull()

            if (last != null && current.startTime < (last.endTime!! + 1.minutes)) {
                if (current.endTime!! > last.endTime) {
                    mergedHistory[mergedHistory.lastIndex] = last.copy(endTime = current.endTime)
                }
            } else {
                mergedHistory.add(current)
            }
        }

        val finalHistory = mergedHistory.filter {
            (it.endTime!! - it.startTime) >= 1.minutes
        }

        sessionRepository.replaceHistory(finalHistory, activeSession)
    }
}