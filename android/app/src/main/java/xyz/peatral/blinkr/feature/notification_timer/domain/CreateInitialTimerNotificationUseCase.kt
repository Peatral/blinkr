package xyz.peatral.blinkr.feature.notification_timer.domain

import android.app.Notification
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.feature.notification_timer.data.TimerNotificationRepository
import javax.inject.Inject

class CreateInitialTimerNotificationUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository,
    private val timerRepository: TimerRepository,
) {
    operator fun invoke(): Notification {
        val currentTimer = timerRepository.timer.value
        return if (currentTimer != null) {
            notificationRepository.builderWithTimer(currentTimer.end)
        } else {
            notificationRepository.builder()
        }.build()
    }
}