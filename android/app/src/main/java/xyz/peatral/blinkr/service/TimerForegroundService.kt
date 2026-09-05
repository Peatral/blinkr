package xyz.peatral.blinkr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.Common
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.data.repository.GlyphRepository
import xyz.peatral.blinkr.domain.FormatTimerUseCase
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "TIMER_CHANNEL"
    }

    @Inject lateinit var glyphRepository: GlyphRepository
    @Inject lateinit var formatTimerUseCase: FormatTimerUseCase

    private val glyphTimerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        glyphRepository.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        glyphTimerScope.cancel()
        glyphRepository.disconnect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.glyph_notification_title))
            .setContentText(getString(R.string.glyph_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        if (timerJob == null || timerJob?.isActive != true) {
            timerJob = glyphTimerScope.launch {
                formatTimerUseCase().collectLatest { formattedTime ->
                    run {
                        val matrixSize = Common.getDeviceMatrixLength()

                        val approxTextHeight = 5
                        val approxTextWidth =
                            4 * 4 + 4 + 1 // 4 numbers a 4 px, 4 paddings, the colon

                        val centerY = (matrixSize - approxTextHeight) / 2
                        val centerX = (matrixSize - approxTextWidth) / 2

                        glyphRepository.displayText(formattedTime, centerX, centerY)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.glyph_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}