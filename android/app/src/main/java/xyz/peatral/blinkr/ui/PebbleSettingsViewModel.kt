package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.datasource.pebble.PebbleMessage
import xyz.peatral.blinkr.data.repository.PebbleSettings
import xyz.peatral.blinkr.data.repository.PebbleSettingsRepository
import javax.inject.Inject

@HiltViewModel
class PebbleSettingsViewModel @Inject constructor(
    private val pebbleSettingsRepository: PebbleSettingsRepository,
    private val pebbleDataSource: PebbleDataSource,
) : ViewModel() {

    val settings = pebbleSettingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PebbleSettings(
            intervalMins = PebbleSettingsRepository.DEFAULT_INTERVAL_MINS
        )
    )

    fun setIntervalMins(intervalMins: Int) {
        viewModelScope.launch {
            pebbleSettingsRepository.setIntervalMins(intervalMins)
            pebbleDataSource.sendMessageToWatch(
                PebbleMessage.UpdateSettings(
                    intervalMins = intervalMins
                )
            )
        }
    }
}
