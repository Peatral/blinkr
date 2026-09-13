package xyz.peatral.blinkr.feature.pebble.domain

import xyz.peatral.blinkr.core.data.repository.Timer
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import javax.inject.Inject

class SyncPebbleTimerUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository,
    private val timerRepository: TimerRepository
) {
    suspend operator fun invoke() {
        pebbleRepository.incomingMessages.collect { message ->
            when (message) {
                is PebbleMessage.RescheduleTimer -> {
                    timerRepository.updateTimer(Timer(message.startTimestamp, message.endTimestamp))
                }
                is PebbleMessage.StopSession -> {
                    timerRepository.updateTimer(null)
                }
                else -> {}
            }
        }
    }
}