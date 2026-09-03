package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.data.pebble.PebbleMessage
import xyz.peatral.blinkr.data.room.SessionEntity
import xyz.peatral.blinkr.repository.SyncRepository
import xyz.peatral.blinkr.repository.TrackingRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

data class TimerUiState(
    val isTimerRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val isWaiting: Boolean = true
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val numberOfDaysDisplayed = 7

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val sessionsByDaysAgo: StateFlow<Map<Int, List<SessionEntity>>> = run {
        val zone = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(zone)
            .date
            .atStartOfDayIn(zone)
        val endOfToday = startOfToday + 1.days
        val startOfNDaysAgo = startOfToday - numberOfDaysDisplayed.days

        syncRepository.getSessionsForTimeframe(startOfNDaysAgo, endOfToday)
            .map { allSessions ->
                (0..<numberOfDaysDisplayed-1).associateWith { daysAgo ->
                    val dayStart = startOfToday - daysAgo.days
                    val dayEnd = dayStart + 1.days
                    allSessions.filter { it.endTime > dayStart && it.startTime < dayEnd }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )
    }

    init {
        viewModelScope.launch {
            trackingRepository.watchMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        val currentUnixTime = System.currentTimeMillis() / 1000L
                        val remaining = (message.timestamp - currentUnixTime).toInt()
                        _uiState.value = TimerUiState(
                            isTimerRunning = true,
                            remainingSeconds = remaining,
                            isWaiting = false
                        )
                    }
                    is PebbleMessage.StopSession -> {
                        _uiState.value = TimerUiState(
                            isTimerRunning = false,
                            remainingSeconds = 0,
                            isWaiting = false
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
