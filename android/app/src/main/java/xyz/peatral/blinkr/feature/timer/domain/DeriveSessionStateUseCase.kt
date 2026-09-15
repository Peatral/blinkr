package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.core.data.EventOrigin
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import xyz.peatral.blinkr.core.data.repository.SessionState
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import javax.inject.Inject

class DeriveSessionStateUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        timerRepository.timerStateUpdates.collectLatest { update ->
            if (update.origin == EventOrigin.LOCAL) {
                when (val next = update.nextState) {
                    is TimerState.Running -> {
                        val currentSession = sessionRepository.sessionState.value
                        if (currentSession is SessionState.Break) {
                            sessionRepository.updateState(
                                SessionState.Active(next.timer.start),
                                EventOrigin.LOCAL
                            )
                        }
                    }
                    is TimerState.Idle -> {
                        val currentSession = sessionRepository.sessionState.value
                        if (currentSession is SessionState.Active && next.timestamp != null) {
                            sessionRepository.updateState(
                                SessionState.Break(startTime = next.timestamp),
                                EventOrigin.LOCAL
                            )
                        }
                    }
                    is TimerState.Expired -> {}
                }
            }
        }
    }
}