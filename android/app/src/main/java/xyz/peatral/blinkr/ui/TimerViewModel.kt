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
import xyz.peatral.blinkr.data.pebble.PebbleMessage
import xyz.peatral.blinkr.data.room.SessionEntity
import xyz.peatral.blinkr.repository.SyncRepository
import xyz.peatral.blinkr.repository.TrackingRepository
import java.util.Calendar
import javax.inject.Inject

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
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartSeconds = calendar.timeInMillis / 1000L
        val endOfTodaySeconds = todayStartSeconds + (24 * 60 * 60)
        val startOf7DaysAgoSeconds = todayStartSeconds - ((numberOfDaysDisplayed - 1) * 24 * 60 * 60)

        syncRepository.getSessionsForTimeframe(startOf7DaysAgoSeconds, endOfTodaySeconds)
            .map { allSessions ->
                (0..<numberOfDaysDisplayed-1).associateWith { daysAgo ->
                    val dayStart = todayStartSeconds - (daysAgo * 24 * 60 * 60)
                    val dayEnd = dayStart + (24 * 60 * 60)
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
