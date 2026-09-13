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

sealed interface TimerState {
    object Idle : TimerState
    data class Running(val timer: Timer) : TimerState
    data class Expired(val timer: Timer) : TimerState
}

@Singleton
class TimerRepository @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope
) {
    private var expirationJob: Job? = null
    private val _currentTimerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState = _currentTimerState.asStateFlow()

    fun updateState(newState: TimerState) {
        _currentTimerState.value = newState
        expirationJob?.cancel()

        if (newState is TimerState.Running) {
            val timer = newState.timer
            val timeRemaining = timer.end - Clock.System.now()
            if (timeRemaining > Duration.ZERO) {
                expirationJob = appScope.launch {
                    delay(timeRemaining)
                    _currentTimerState.value = TimerState.Expired(timer)
                }
            } else {
                _currentTimerState.value = TimerState.Expired(timer)
            }
        }
    }

    fun compareAndSetState(expect: TimerState, update: TimerState) {
        _currentTimerState.compareAndSet(expect, update)
    }
}