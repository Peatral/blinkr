package xyz.peatral.blinkr.core.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import xyz.peatral.blinkr.core.data.AppState
import xyz.peatral.blinkr.core.data.EventOrigin
import xyz.peatral.blinkr.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class Timer(val start: Instant, val end: Instant)

@Serializable
sealed interface TimerState {
    @Serializable
    data class Idle(val timestamp: Instant? = Clock.System.now()) : TimerState
    @Serializable
    data class Running(val timer: Timer) : TimerState
    @Serializable
    data class Expired(val timer: Timer) : TimerState
}

data class TimerStateUpdate(val prevState: TimerState, val nextState: TimerState, val origin: EventOrigin)

@Singleton
class TimerRepository @Inject constructor(
    private val dataStore: DataStore<AppState>,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private var expirationJob: Job? = null

    private val _currentTimerState = MutableStateFlow<TimerState>(TimerState.Idle(null))
    val timerState = _currentTimerState.asStateFlow()

    private val _timerStateUpdates = MutableSharedFlow<TimerStateUpdate>(extraBufferCapacity = 10)
    val timerStateUpdates = _timerStateUpdates.asSharedFlow()

    init {
        appScope.launch {
            val appState = dataStore.data.first()
            val savedTimerState = appState.timerState

            val restoredState = if (savedTimerState is TimerState.Running) {
                if (Clock.System.now() >= savedTimerState.timer.end) {
                    TimerState.Expired(savedTimerState.timer)
                } else {
                    savedTimerState
                }
            } else {
                savedTimerState
            }

            _currentTimerState.value = restoredState

            if (restoredState is TimerState.Running) {
                startExpirationJob(restoredState.timer)
            }
        }
    }

    fun updateState(newState: TimerState, origin: EventOrigin = EventOrigin.LOCAL) {
        val oldState = _currentTimerState.value
        if (oldState == newState) return

        _currentTimerState.value = newState
        persistStateAsync(newState)

        _timerStateUpdates.tryEmit(
            TimerStateUpdate(prevState = oldState, nextState = newState, origin = origin)
        )

        expirationJob?.cancel()

        if (newState is TimerState.Running) {
            startExpirationJob(newState.timer)
        }
    }

    private fun startExpirationJob(timer: Timer) {
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

    private fun persistStateAsync(state: TimerState) {
        appScope.launch {
            dataStore.updateData { currentAppState ->
                currentAppState.copy(
                    timerState = state
                )
            }
        }
    }
}