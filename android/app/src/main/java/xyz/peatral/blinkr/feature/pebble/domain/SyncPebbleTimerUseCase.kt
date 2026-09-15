package xyz.peatral.blinkr.feature.pebble.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.EventOrigin
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import xyz.peatral.blinkr.core.data.repository.SessionState
import xyz.peatral.blinkr.core.data.repository.Timer
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import javax.inject.Inject

class SyncPebbleTimerUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository,
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() = coroutineScope {
        launch {
            pebbleRepository.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        timerRepository.updateState(
                            TimerState.Running(
                                timer = Timer(message.startTimestamp, message.endTimestamp),
                            ),
                            EventOrigin.REMOTE,
                        )
                    }
                    is PebbleMessage.StartSession -> {
                        sessionRepository.updateState(
                            SessionState.Active(message.startTimestamp),
                            EventOrigin.REMOTE,
                        )
                    }
                    is PebbleMessage.StopSession -> {
                        // Stopping a session also stops active timers
                        timerRepository.updateState(
                            TimerState.Idle(message.endTimestamp),
                            EventOrigin.REMOTE
                        )
                        sessionRepository.updateState(
                            SessionState.Break(message.endTimestamp),
                            EventOrigin.REMOTE
                        )
                    }
                    else -> {}
                }
            }
        }

        launch {
            sessionRepository.sessionStateUpdates.collect { update ->
                if (update.origin == EventOrigin.REMOTE) {
                    return@collect
                }

                val prev = update.prevState
                val next = update.nextState

                when (next) {
                    is SessionState.Active -> {
                        pebbleRepository.sendMessageToWatch(
                            PebbleMessage.StartSession(next.startTime)
                        )
                    }
                    is SessionState.Break -> {
                        if (prev is SessionState.Active) {
                            pebbleRepository.sendMessageToWatch(
                                PebbleMessage.StopSession(
                                    startTimestamp = prev.startTime,
                                    endTimestamp = next.startTime,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}