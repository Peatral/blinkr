package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.data.repository.SyncRepository
import xyz.peatral.blinkr.data.repository.SyncState
import xyz.peatral.blinkr.domain.CurrentTimeUseCase
import xyz.peatral.blinkr.domain.FormatTimerUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

data class TimerUiState(
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val syncRepository: SyncRepository,
    private val currentTimeUseCase: CurrentTimeUseCase,
) : ViewModel() {
    private val numberOfDaysDisplayed = 7

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val currentTime = currentTimeUseCase()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionsByDaysAgo: StateFlow<Map<Int, List<SessionEntity>>> = currentTime
        .map { now ->
            val zone = TimeZone.currentSystemDefault()
            now.toLocalDateTime(zone).date.atStartOfDayIn(zone)
        }
        .distinctUntilChanged()
        .flatMapLatest { startOfToday ->
            val endOfToday = startOfToday + 1.days
            val startOfNDaysAgo = startOfToday - (numberOfDaysDisplayed - 1).days

            syncRepository.getSessionsForTimeframe(startOfNDaysAgo, endOfToday)
                .map { allSessions ->
                    (0..<numberOfDaysDisplayed).associateWith { daysAgo ->
                        val dayStart = startOfToday - daysAgo.days
                        val dayEnd = dayStart + 1.days
                        allSessions.filter { it.endTime > dayStart && it.startTime < dayEnd }
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val timerText = formatTimerUseCase()

    fun refreshSessions() {
        syncRepository.requestSync()
        _uiState.value = _uiState.value.copy(isRefreshing = true)
    }

    init {
        syncRepository.requestSync();
        viewModelScope.launch {
            syncRepository.syncState.collect { state ->
                when (state) {
                    is SyncState.Idle -> _uiState.value = _uiState.value.copy(isRefreshing = false)
                    is SyncState.Syncing -> _uiState.value = _uiState.value.copy(isRefreshing = true)
                }
            }
        }
    }
}
