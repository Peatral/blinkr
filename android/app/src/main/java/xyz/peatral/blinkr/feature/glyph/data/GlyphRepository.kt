package xyz.peatral.blinkr.feature.glyph.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.peatral.blinkr.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class GlyphRepository @Inject constructor(
    private val glyphDataSource: GlyphDataSource,
    @ApplicationScope private val appScope: CoroutineScope
) {
    var references = 0

    private val hardwareMutex = Mutex()

    suspend fun connect() {
        withHardwareLock {
            if (references <= 0) {
                glyphDataSource.connect()
            }
            references++
        }
    }

    fun disconnect() {
        references = 0.coerceAtLeast(references - 1)
        if (references <= 0) {
            appScope.launch {
                delay(100.milliseconds)
                if (references <= 0) {
                    clearDisplay()
                    glyphDataSource.disconnect()
                }
            }
        }
    }

    fun turnOnAll(brightness: Int = 255) {
        glyphDataSource.turnOnAll(brightness)
    }

    fun displayText(text: String, x: Int, y: Int, brightness: Int = 255) {
        glyphDataSource.displayText(
            text = text,
            x = x,
            y = y,
            brightness = brightness,
        )
    }

    fun clearDisplay() {
        glyphDataSource.clearDisplay()
    }

    suspend fun <T> withHardwareLock(block: suspend () -> T): T {
        return hardwareMutex.withLock { block() }
    }

    fun tryHardwareLock(block: () -> Unit) {
        if (hardwareMutex.tryLock()) {
            try {
                block()
            } finally {
                hardwareMutex.unlock()
            }
        }
    }
}
