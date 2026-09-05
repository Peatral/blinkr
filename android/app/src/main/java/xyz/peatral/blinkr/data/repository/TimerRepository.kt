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

@Singleton
class TimerRepository @Inject constructor(
    private val pebbleDataSource: PebbleDataSource,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _nextWakeup = MutableStateFlow<Instant?>(null)
    val nextWakeup = _nextWakeup.asStateFlow()

    init {
        repoScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        _nextWakeup.value = message.next_wakeup
                    }
                    is PebbleMessage.StopSession -> {
                        _nextWakeup.value = null
                    }
                    else -> {}
                }
            }
        }

        repoScope.launch {
            _nextWakeup.collectLatest { nextWakeup ->
                if (nextWakeup != null) {
                    val currentTime = Clock.System.now()

                    if (currentTime < nextWakeup) {
                        delay(nextWakeup - currentTime)
                    }

                    _nextWakeup.value = null
                }
            }
        }
    }
}