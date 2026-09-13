package xyz.peatral.blinkr.feature.notification_timer.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import xyz.peatral.blinkr.MainActivity
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.core.data.repository.TimerRepository
import javax.inject.Inject
import kotlin.time.Instant

class TimerNotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timerRepository: TimerRepository
) {
    companion object {
        const val CHANNEL_ID = "TIMER_CHANNEL"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val baseBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.reminder_notification_title))
        .setContentText(context.getString(R.string.reminder_notification_text))
        .setSmallIcon(R.drawable.ic_notification_timer)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setRequestPromotedOngoing(true)
        .setContentIntent(createPendingIntent())

    init {
        createNotificationChannel()
    }

    fun createInitialNotification(): Notification {
        val currentTimer = timerRepository.timer.value
        return if (currentTimer != null) {
            buildWithTimer(currentTimer.end)
        } else {
            baseBuilder.build()
        }
    }

    suspend fun startUpdatingNotification(notificationId: Int) {
        timerRepository.timer.collectLatest { timer ->
            if (timer != null) {
                val notification = buildWithTimer(timer.end)
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    fun clear(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun buildWithTimer(timerWhen: Instant): Notification {
        return baseBuilder
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(timerWhen.toEpochMilliseconds())
            .setShowWhen(true)
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = context.getString(R.string.reminder_notification_channel_description)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}