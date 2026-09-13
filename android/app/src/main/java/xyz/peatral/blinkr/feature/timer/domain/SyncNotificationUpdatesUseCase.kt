package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.feature.timer.data.TimerNotificationRepository
import javax.inject.Inject

class SyncNotificationUpdatesUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository,
    private val timerRepository: TimerRepository,
) {
    suspend operator fun invoke(notificationId: Int) {
        try {
            timerRepository.timer.collectLatest { timer ->
                if (timer != null) {
                    val notification = notificationRepository.builderWithTimer(timer.end).build()
                    notificationRepository.update(notificationId, notification)
                }
            }
        } finally {
            notificationRepository.clear(notificationId)
        }
    }
}