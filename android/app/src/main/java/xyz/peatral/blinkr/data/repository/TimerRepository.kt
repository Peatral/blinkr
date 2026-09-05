package xyz.peatral.blinkr.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.datasource.pebble.PebbleMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

class Timer(val start: Instant, val end: Instant)

@Singleton
class TimerRepository @Inject constructor(
    private val pebbleDataSource: PebbleDataSource,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _currentTimer = MutableStateFlow<Timer?>(null)
    val timer = _currentTimer.asStateFlow()

    init {
        repoScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        _currentTimer.value = Timer(message.startTimestamp, message.endTimestamp)
                    }
                    is PebbleMessage.StopSession -> {
                        _currentTimer.value = null
                    }
                    else -> {}
                }
            }
        }

        repoScope.launch {
            _currentTimer.collectLatest { currentTimer ->
                if (currentTimer != null) {
                    val currentTime = Clock.System.now()

                    if (currentTime < currentTimer.end) {
                        delay(currentTimer.end - currentTime)
                    }

                    _currentTimer.value = null
                }
            }
        }
    }
}