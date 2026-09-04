package xyz.peatral.blinkr.service

import dagger.hilt.android.AndroidEntryPoint
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import xyz.peatral.blinkr.data.pebble.PebbleDataSource
import xyz.peatral.blinkr.repository.SyncRepository
import xyz.peatral.blinkr.repository.TrackingRepository
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class PebbleListenerService : BasePebbleListenerService() {

    @Inject
    lateinit var pebbleDataSource: PebbleDataSource

    // Injected purely to ensure Hilt initializes them and their
    // init {} blocks start collecting from pebbleDataSource
    @Inject
    lateinit var trackingRepository: TrackingRepository

    @Inject
    lateinit var syncRepository: SyncRepository

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier
    ): ReceiveResult {
        pebbleDataSource.processIncomingMessage(data)
        return ReceiveResult.Ack
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        pebbleDataSource.setAppOpen(watchappUUID, true)
    }

    override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
        pebbleDataSource.setAppOpen(watchappUUID, false)
    }
}
