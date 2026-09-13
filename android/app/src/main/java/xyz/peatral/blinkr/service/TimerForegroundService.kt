package xyz.peatral.blinkr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.MainActivity
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import xyz.peatral.blinkr.core.domain.FormatTimerUseCase
import xyz.peatral.blinkr.feature.glyph.domain.UpdateGlyphDisplayUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@AndroidEntryPoint
class TimerForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "TIMER_CHANNEL"
    }

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var formatTimerUseCase: FormatTimerUseCase
    @Inject lateinit var updateGlyphDisplayUseCase: UpdateGlyphDisplayUseCase

    private val glyphTimerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Blinkr:TimerWakeLock")
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        glyphTimerScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationId = 1
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.reminder_notification_title))
            .setContentText(getString(R.string.reminder_notification_text))
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)

        val currentTimer = timerRepository.timer.value
        if (currentTimer != null) {
            notificationBuilder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(currentTimer.end.toEpochMilliseconds())
                .setShowWhen(true)
        }

        startForeground(notificationId, notificationBuilder.build())

        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(10.hours.inWholeMilliseconds)
            }
        }

        if (timerJob == null || timerJob?.isActive != true) {
            timerJob = glyphTimerScope.launch {
                launch {
                    manageNotificationProgress(notificationId, notificationBuilder)
                }

                launch {
                    updateGlyphDisplayUseCase()
                }
            }
        }

        return START_NOT_STICKY
    }

    suspend fun manageNotificationProgress(notificationId: Int, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        timerRepository.timer.collectLatest { timer ->
            if (timer != null) {
                notificationBuilder
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(timer.end.toEpochMilliseconds())
                    .setShowWhen(true)

                notificationManager.notify(
                    notificationId,
                    notificationBuilder.build()
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = getString(R.string.reminder_notification_channel_description)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
