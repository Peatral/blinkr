package xyz.peatral.blinkr.feature.pebble.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import xyz.peatral.blinkr.core.domain.AppInitializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PebbleInitializer @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val syncPebbleDataUseCase: SyncPebbleDataUseCase
) : AppInitializer {

    override fun initialize() {
        appScope.launch {
            syncPebbleDataUseCase()
        }
    }
}