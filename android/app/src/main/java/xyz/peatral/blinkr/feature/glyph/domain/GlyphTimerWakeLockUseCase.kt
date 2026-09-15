package xyz.peatral.blinkr.feature.glyph.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.data.repository.TimerState
import xyz.peatral.blinkr.core.system.WakeLockManager
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class GlyphTimerWakeLockUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val wakeLockManager: WakeLockManager,
) {
    companion object {
        const val WAKELOCK_TAG = "Blinkr:GlyphTimerWakeLock"
        val WAKELOCK_DURATION = 10.hours
    }

    suspend operator fun invoke() = coroutineScope {
        try {
            timerRepository.timerState
                .map { it !is TimerState.Idle }
                .distinctUntilChanged()
                .collectLatest { needsWakeLock ->
                    if (needsWakeLock) {
                        wakeLockManager.acquire(WAKELOCK_TAG, WAKELOCK_DURATION)
                    } else {
                        wakeLockManager.release(WAKELOCK_TAG)
                    }
                }
        } finally {
            wakeLockManager.release(WAKELOCK_TAG)
        }
    }
}