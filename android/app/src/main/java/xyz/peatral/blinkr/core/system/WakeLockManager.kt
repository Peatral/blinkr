package xyz.peatral.blinkr.core.system

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val wakeLocks = ConcurrentHashMap<String, PowerManager.WakeLock>()

    fun acquire(tag: String, duration: Duration) {
        val wakeLock = wakeLocks.getOrPut(tag) {
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
        }

        wakeLock.acquire(duration.inWholeMilliseconds)
    }

    fun release(tag: String) {
        wakeLocks[tag]?.let { wakeLock ->
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}