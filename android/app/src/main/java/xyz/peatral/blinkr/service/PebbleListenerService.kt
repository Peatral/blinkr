package xyz.peatral.blinkr.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.repository.SyncRepository
import xyz.peatral.blinkr.data.repository.TimerRepository
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class PebbleListenerService : BasePebbleListenerService() {
    @Inject
    lateinit var pebbleDataSource: PebbleDataSource

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var syncRepository: SyncRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val serviceIntent = Intent(this, TimerForegroundService::class.java)

        scope.launch {
            timerRepository.nextWakeup.collect { nextWakeup ->
                if (nextWakeup != null) {
                    ContextCompat.startForegroundService(this@PebbleListenerService, serviceIntent)
                } else {
                    stopService(serviceIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier
    ): ReceiveResult {
        val handled = pebbleDataSource.processIncomingMessage(watchappUUID, data)
        return if (handled) {
            ReceiveResult.Ack
        } else {
            ReceiveResult.Nack
        }
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        pebbleDataSource.setAppOpen(watchappUUID, true)
    }

    override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
        pebbleDataSource.setAppOpen(watchappUUID, false)
    }
}
