package xyz.peatral.blinkr.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.GlyphDataSource
import xyz.peatral.blinkr.data.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.pebble.PebbleMessage
import xyz.peatral.blinkr.service.TimerForegroundService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Singleton
class TrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pebbleDataSource: PebbleDataSource,
    private val glyphDataSource: GlyphDataSource,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    init {
        glyphDataSource.connect()

        repoScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.RescheduleTimer -> {
                        val remainingSeconds = message.next_wakeup - Clock.System.now()
                        startTimer(remainingSeconds)
                    }
                    is PebbleMessage.StopSession -> stopTimer()
                    else -> {}
                }
            }
        }
    }

    val watchMessages: Flow<PebbleMessage> = pebbleDataSource.incomingMessages

    private fun startTimer(durationSeconds: Duration) {
        timerJob?.cancel()

        val serviceIntent = Intent(context, TimerForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        timerJob = repoScope.launch {
            var remaining = durationSeconds

            while (remaining >= 0.seconds) {
                val timeString = remaining.toComponents { minutes, seconds, _ -> String.format("%02d:%02d", minutes, seconds) }

                glyphDataSource.displayTime(timeString)

                delay(1.seconds)
                remaining -= 1.seconds
            }

            glyphDataSource.clearDisplay()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        glyphDataSource.clearDisplay()

        val serviceIntent = Intent(context, TimerForegroundService::class.java)
        context.stopService(serviceIntent)
    }

    fun cleanup() {
        stopTimer()
        glyphDataSource.disconnect()
        pebbleDataSource.cleanup()
    }
}