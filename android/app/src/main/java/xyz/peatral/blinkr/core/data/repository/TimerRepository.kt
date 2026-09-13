package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

data class Timer(val start: Instant, val end: Instant)

@Singleton
class TimerRepository @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope
) {
    private var expirationJob: Job? = null
    private val _currentTimer = MutableStateFlow<Timer?>(null)
    val timer = _currentTimer.asStateFlow()

    fun updateTimer(timer: Timer?) {
        _currentTimer.value = timer
        expirationJob?.cancel()

        if (timer != null) {
            val timeRemaining = timer.end - Clock.System.now()
            if (timeRemaining > Duration.ZERO) {
                expirationJob = appScope.launch {
                    delay(timeRemaining)
                    _currentTimer.value = null
                }
            } else {
                _currentTimer.value = null
            }
        }
    }
}