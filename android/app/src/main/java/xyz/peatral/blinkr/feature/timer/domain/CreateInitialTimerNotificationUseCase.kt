package xyz.peatral.blinkr.feature.timer.domain

import android.app.Notification
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.feature.timer.data.TimerNotificationRepository
import javax.inject.Inject

class CreateInitialTimerNotificationUseCase @Inject constructor(
    private val notificationRepository: TimerNotificationRepository,
    private val timerRepository: TimerRepository,
) {
    operator fun invoke(): Notification {
        val currentTimerState = timerRepository.timerState.value
        return if (currentTimerState is TimerState.Running) {
            notificationRepository.builderWithTimer(currentTimerState.timer.end)
        } else {
            notificationRepository.builder()
        }.build()
    }
}