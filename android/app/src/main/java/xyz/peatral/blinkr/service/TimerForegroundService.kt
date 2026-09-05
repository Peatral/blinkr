package xyz.peatral.blinkr.service

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
import xyz.peatral.blinkr.data.repository.TimerRepository
import xyz.peatral.blinkr.domain.CurrentTimeUseCase
import xyz.peatral.blinkr.domain.FormatTimerUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class TimerForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "TIMER_CHANNEL"
    }

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var glyphRepository: GlyphRepository
    @Inject lateinit var formatTimerUseCase: FormatTimerUseCase

    @Inject lateinit var currentTimeUseCase: CurrentTimeUseCase

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
        val notificationId = 1
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.glyph_notification_title))
            .setContentText(getString(R.string.glyph_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        startForeground(notificationId, notificationBuilder.build())

        if (timerJob == null || timerJob?.isActive != true) {
            timerJob = glyphTimerScope.launch {
                launch {
                    manageNotificationProgress(notificationId, notificationBuilder)
                }

                launch {
                    manageGlyphTimer()
                }
            }
        }

        return START_NOT_STICKY
    }

    suspend fun manageNotificationProgress(notificationId: Int, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        timerRepository.timer.collectLatest { timer ->
            if (timer != null) {
                currentTimeUseCase(1.seconds, timer.end).collectLatest { currentTime ->
                    run {
                        val duration = timer.end - timer.start
                        val elapsed = currentTime - timer.start
                        notificationBuilder.setProgress(
                            duration.inWholeSeconds.toInt(),
                            elapsed.inWholeSeconds.toInt(),
                            false
                        )
                        notificationManager.notify(
                            notificationId,
                            notificationBuilder.build()
                        )
                    }
                }
            } else {
                notificationBuilder.setProgress(0, 0, true)
                notificationManager.notify(notificationId, notificationBuilder.build())
            }
        }
    }

    suspend fun manageGlyphTimer() {
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