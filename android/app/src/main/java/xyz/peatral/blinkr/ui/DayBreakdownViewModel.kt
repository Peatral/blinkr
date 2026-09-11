package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.data.repository.SyncRepository
import xyz.peatral.blinkr.domain.FormatDurationUseCase
import xyz.peatral.blinkr.domain.FormatLocalDateUseCase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@HiltViewModel(assistedFactory = DayBreakdownViewModel.Factory::class)
class DayBreakdownViewModel @AssistedInject constructor(
    @Assisted val targetDate: LocalDate,
    private val syncRepository: SyncRepository,
    private val formatDurationUseCase: FormatDurationUseCase,
    private val formatLocalDateUseCase: FormatLocalDateUseCase,
) : ViewModel() {
    private val zone = TimeZone.currentSystemDefault()
    private val dayStart = targetDate.atStartOfDayIn(zone)
    private val dayEnd = dayStart + 1.days

    val sessions: StateFlow<List<SessionEntity>> = syncRepository
        .getSessionsForTimeframe(dayStart, dayEnd)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val formatDuration = { duration: Duration -> formatDurationUseCase(duration) }
    val formatDate = { date: LocalDate -> formatLocalDateUseCase(date) }

    @AssistedFactory
    interface Factory {
        fun create(targetDate: LocalDate): DayBreakdownViewModel
    }
}