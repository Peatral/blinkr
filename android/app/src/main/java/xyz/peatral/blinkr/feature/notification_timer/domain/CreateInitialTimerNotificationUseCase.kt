package xyz.peatral.blinkr.feature.notification_timer.domain

import android.app.Notification
import xyz.peatral.blinkr.feature.notification_timer.data.TimerNotificationRepository
import javax.inject.Inject

class CreateInitialTimerNotificationUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository
) {
    operator fun invoke(): Notification {
        return notificationRepository.createInitialNotification()
    }
}