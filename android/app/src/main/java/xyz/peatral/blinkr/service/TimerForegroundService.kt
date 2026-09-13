package xyz.peatral.blinkr.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.feature.glyph.domain.UpdateGlyphDisplayUseCase
import xyz.peatral.blinkr.feature.notification_timer.domain.CreateInitialTimerNotificationUseCase
import xyz.peatral.blinkr.feature.notification_timer.domain.SyncNotificationUpdatesUseCase
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
    }

    @Inject lateinit var createInitialNotification: CreateInitialTimerNotificationUseCase
    @Inject lateinit var syncNotificationUpdates: SyncNotificationUpdatesUseCase
    @Inject lateinit var updateGlyphDisplay: UpdateGlyphDisplayUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createInitialNotification())

        if (timerJob == null || timerJob?.isActive != true) {
            timerJob = serviceScope.launch {
                launch { syncNotificationUpdates(NOTIFICATION_ID) }
                launch { updateGlyphDisplay() }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
