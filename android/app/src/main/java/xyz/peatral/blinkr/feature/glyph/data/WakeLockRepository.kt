package xyz.peatral.blinkr.feature.glyph.data

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration

class WakeLockRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(tag: String, duration: Duration) {
        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
        }

        wakeLock?.let {
            if (!it.isHeld) it.acquire(duration.inWholeMilliseconds)
        }
    }

    fun release() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }
}