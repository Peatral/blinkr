package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

data class Timer(val start: Instant, val end: Instant)

@Singleton
class TimerRepository @Inject constructor() {
    private val _currentTimer = MutableStateFlow<Timer?>(null)
    val timer = _currentTimer.asStateFlow()

    fun updateTimer(timer: Timer?) {
        _currentTimer.value = timer
    }
}