package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.pebble.PebbleMessage
import xyz.peatral.blinkr.data.room.SessionEntity
import xyz.peatral.blinkr.repository.SyncRepository
import xyz.peatral.blinkr.repository.TrackingRepository
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

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val todaySessions: StateFlow<List<SessionEntity>> = syncRepository.getTodaySessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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