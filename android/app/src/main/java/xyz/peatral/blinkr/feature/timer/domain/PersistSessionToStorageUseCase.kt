package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import xyz.peatral.blinkr.core.data.repository.SessionState
import xyz.peatral.blinkr.core.domain.ReconcileSessionsUseCase
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PersistSessionToStorageUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val reconcileSessionsUseCase: ReconcileSessionsUseCase
) {
    suspend operator fun invoke() {
        sessionRepository.sessionStateUpdates.collectLatest { update ->
            val prev = update.prevState
            val next = update.nextState

            when (next) {
                is SessionState.Active -> {
                    sessionRepository.saveSession(
                        SessionEntity(startTime = next.startTime, endTime = null)
                    )
                }
                is SessionState.Break -> {
                    if (prev is SessionState.Active) {
                        val sessionStart = prev.startTime
                        val sessionEnd = next.startTime

                        if (isValidSession(sessionStart, sessionEnd)) {
                            sessionRepository.saveSession(SessionEntity(sessionStart, sessionEnd))
                        } else {
                            sessionRepository.deleteUnfinishedSession()
                        }

                        reconcileSessionsUseCase(Clock.System.now())
                    }
                }
            }
        }
    }

    private fun isValidSession(start: Instant, end: Instant): Boolean =
        start < end && end - start >= 1.minutes
}