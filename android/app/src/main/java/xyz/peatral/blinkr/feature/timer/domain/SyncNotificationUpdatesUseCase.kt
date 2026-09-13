package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.feature.timer.data.TimerNotificationRepository
import javax.inject.Inject

class SyncNotificationUpdatesUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository,
    private val timerRepository: TimerRepository,
) {
    suspend operator fun invoke(notificationId: Int) {
        try {
            timerRepository.timerState.collectLatest { timerState ->
                when (timerState) {
                    is TimerState.Running -> {
                        val timer = timerState.timer
                        val notification = notificationRepository.builderWithTimer(timer.end).build()
                        notificationRepository.update(notificationId, notification)
                    }
                    else -> {}
                }
            }
        } finally {
            notificationRepository.clear(notificationId)
        }
    }
}