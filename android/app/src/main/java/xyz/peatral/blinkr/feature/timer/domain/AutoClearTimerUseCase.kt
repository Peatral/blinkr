package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import javax.inject.Inject
import kotlin.time.Clock

class AutoClearExpiredTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository
) {
    suspend operator fun invoke() {
        timerRepository.timer.collectLatest { currentTimer ->
            if (currentTimer != null) {
                val currentTime = Clock.System.now()

                if (currentTime < currentTimer.end) {
                    delay(currentTimer.end - currentTime)
                }

                timerRepository.updateTimer(null)
            }
        }
    }
}
