package xyz.peatral.blinkr.feature.pebble.service

import dagger.hilt.android.AndroidEntryPoint
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.domain.OrchestrateTimerServiceUseCase
import xyz.peatral.blinkr.feature.pebble.domain.HandlePebbleMessageUseCase
import xyz.peatral.blinkr.feature.pebble.domain.SyncPebbleDataUseCase
import xyz.peatral.blinkr.feature.pebble.domain.SyncPebbleTimerUseCase
import xyz.peatral.blinkr.feature.pebble.domain.UpdatePebbleAppVisibilityUseCase
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class PebbleListenerService : BasePebbleListenerService() {
    @Inject lateinit var handlePebbleMessage: HandlePebbleMessageUseCase
    @Inject lateinit var updatePebbleAppVisibility: UpdatePebbleAppVisibilityUseCase
    @Inject lateinit var syncPebbleData: SyncPebbleDataUseCase
    @Inject lateinit var syncPebbleTimer: SyncPebbleTimerUseCase
    @Inject lateinit var orchestrateTimerService: OrchestrateTimerServiceUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            launch { orchestrateTimerService() }
            launch { syncPebbleData() }
            launch { syncPebbleTimer() }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier
    ): ReceiveResult {
        val handled = handlePebbleMessage(watchappUUID, data)
        return if (handled) {
            ReceiveResult.Ack
        } else {
            ReceiveResult.Nack
        }
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        updatePebbleAppVisibility(watchappUUID, true)
    }

    override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
        updatePebbleAppVisibility(watchappUUID, false)
    }
}