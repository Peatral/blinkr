package xyz.peatral.blinkr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.datasource.pebble.PebbleMessage
import xyz.peatral.blinkr.data.repository.Settings
import xyz.peatral.blinkr.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class PebbleSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pebbleDataSource: PebbleDataSource,
) : ViewModel() {

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings(
            intervalMins = SettingsRepository.DEFAULT_INTERVAL_MINS
        )
    )

    fun setIntervalMins(intervalMins: Int) {
        viewModelScope.launch {
            settingsRepository.setIntervalMins(intervalMins)
            pebbleDataSource.sendMessageToWatch(
                PebbleMessage.UpdateSettings(
                    intervalMins = intervalMins
                )
            )
        }
    }
}
