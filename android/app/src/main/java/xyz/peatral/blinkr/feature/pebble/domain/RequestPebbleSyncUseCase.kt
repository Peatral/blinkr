package xyz.peatral.blinkr.feature.pebble.domain

import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.core.data.repository.SyncRepository
import xyz.peatral.blinkr.core.data.repository.SyncState
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import javax.inject.Inject

class RequestPebbleSyncUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke() {
        syncRepository.updateSyncState(SyncState.Syncing(0.0f))
        pebbleRepository.sendMessageToWatch(PebbleMessage.RequestSync)
    }
}