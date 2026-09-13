package xyz.peatral.blinkr.feature.pebble.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.repository.EventOrigin
import xyz.peatral.blinkr.core.data.repository.Timer
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import javax.inject.Inject

class SyncPebbleTimerUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository,
    private val timerRepository: TimerRepository
) {
    suspend operator fun invoke() = coroutineScope {
        launch {
            pebbleRepository.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        timerRepository.updateState(
                            TimerState.Running(
                                timer = Timer(message.startTimestamp, message.endTimestamp)
                            ),
                            EventOrigin.REMOTE,
                        )
                    }
                    is PebbleMessage.StopSession -> {
                        timerRepository.updateState(
                            TimerState.Idle(message.endTimestamp),
                            EventOrigin.REMOTE,
                        )
                    }
                    else -> {}
                }
            }
        }

        launch {
            timerRepository.timerStateUpdates.collect { (prevState, nextState, origin) ->
                if (origin == EventOrigin.LOCAL) {
                    when (nextState) {
                        is TimerState.Running -> {
                            pebbleRepository.sendMessageToWatch(PebbleMessage.StartSession(nextState.timer.start))
                        }
                        is TimerState.Idle -> {
                            if (nextState.timestamp == null || prevState !is TimerState.Running) {
                                return@collect
                            }
                            pebbleRepository.sendMessageToWatch(PebbleMessage.StopSession(
                                startTimestamp = prevState.timer.start,
                                endTimestamp = nextState.timestamp,
                            ))
                        }
                        is TimerState.Expired -> {}
                    }
                }
            }
        }
    }
}
