package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

data class Timer(val start: Instant, val end: Instant)

data class TimerStateUpdate(val prevState: TimerState, val nextState: TimerState, val origin: EventOrigin)

sealed interface TimerState {
    data class Idle(val timestamp: Instant? = Clock.System.now()) : TimerState
    data class Running(val timer: Timer) : TimerState
    data class Expired(val timer: Timer) : TimerState
}

enum class EventOrigin {
    LOCAL, REMOTE
}

@Singleton
class TimerRepository @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope
) {
    private var expirationJob: Job? = null
    private val _currentTimerState = MutableStateFlow<TimerState>(TimerState.Idle(null))
    val timerState = _currentTimerState.asStateFlow()

    private val _timerStateUpdates = MutableSharedFlow<TimerStateUpdate>(extraBufferCapacity = 10)
    val timerStateUpdates = _timerStateUpdates.asSharedFlow()

    fun updateState(newState: TimerState, origin: EventOrigin = EventOrigin.LOCAL) {
        val oldState = _currentTimerState.value
        _currentTimerState.value = newState
        _timerStateUpdates.tryEmit(TimerStateUpdate(
            prevState = oldState,
            nextState = newState,
            origin = origin
        ))

        expirationJob?.cancel()

        if (newState is TimerState.Running) {
            val timer = newState.timer
            val timeRemaining = timer.end - Clock.System.now()
            if (timeRemaining > Duration.ZERO) {
                expirationJob = appScope.launch {
                    delay(timeRemaining)
                    updateState(TimerState.Expired(timer), EventOrigin.LOCAL)
                }
            } else {
                updateState(TimerState.Expired(timer), EventOrigin.LOCAL)
            }
        }
    }
}