package xyz.peatral.blinkr.core.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.service.TimerForegroundService
import javax.inject.Inject

class OrchestrateTimerServiceUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        val serviceIntent = Intent(context, TimerForegroundService::class.java)
        timerRepository.timer.collect { timer ->
            if (timer != null) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.stopService(serviceIntent)
            }
        }
    }
}