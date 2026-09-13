package xyz.peatral.blinkr.feature.notification_timer.domain

import xyz.peatral.blinkr.feature.notification_timer.data.TimerNotificationRepository
import javax.inject.Inject

class SyncNotificationUpdatesUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository
) {
    suspend operator fun invoke(notificationId: Int) {
        try {
            notificationRepository.startUpdatingNotification(notificationId)
        } finally {
            notificationRepository.clear(notificationId)
        }
    }
}