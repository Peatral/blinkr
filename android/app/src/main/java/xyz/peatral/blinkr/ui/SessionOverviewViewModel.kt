package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.repository.SyncRepository
import xyz.peatral.blinkr.data.repository.SyncState
import xyz.peatral.blinkr.domain.CurrentTimeUseCase
import xyz.peatral.blinkr.domain.FormatTimerUseCase
import javax.inject.Inject

data class TimerUiState(
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class SessionOverviewViewModel @Inject constructor(
    private val formatTimerUseCase: FormatTimerUseCase,
    private val syncRepository: SyncRepository,
    private val currentTimeUseCase: CurrentTimeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val currentTime = currentTimeUseCase()

    val pagedTimeline = Pager(
        config = PagingConfig(
            pageSize = 14,
            initialLoadSize = 28
        ),
        pagingSourceFactory = { TimelinePagingSource(syncRepository) }
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
