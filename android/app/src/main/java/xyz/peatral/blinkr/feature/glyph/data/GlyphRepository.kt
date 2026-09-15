package xyz.peatral.blinkr.feature.glyph.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    private var appReferences = 0
    private var toyReferences = 0

    private val hardwareMutex = Mutex()

    suspend fun connect(mode: GlyphMode = GlyphMode.APP) {
        withHardwareLock {
            if (appReferences == 0 && toyReferences == 0) {
                glyphDataSource.connect()
            }
            if (mode == GlyphMode.TOY) {
                toyReferences++
            } else {
                appReferences++
            }
        }
    }

    fun disconnect(mode: GlyphMode = GlyphMode.APP) {
        appScope.launch {
            val shouldDisconnectAll = withHardwareLock {
                if (mode == GlyphMode.TOY) {
                    toyReferences = 0.coerceAtLeast(toyReferences - 1)
                    if (toyReferences == 0) {
                        clearDisplay(mode = GlyphMode.TOY)
                    }
                } else {
                    appReferences = 0.coerceAtLeast(appReferences - 1)
                    if (appReferences == 0) {
                        clearDisplay(mode = GlyphMode.APP)
                        glyphDataSource.closeAppMatrix()
                    }
                }

                appReferences == 0 && toyReferences == 0
            }

            if (shouldDisconnectAll) {
                delay(100.milliseconds)
                withHardwareLock {
                    if (appReferences == 0 && toyReferences == 0) {
                        glyphDataSource.disconnect()
                    }
                }
            }
        }
    }

    fun turnOnAll(brightness: Int = 255, mode: GlyphMode = GlyphMode.APP) {
        glyphDataSource.turnOnAll(brightness, mode)
    }

    fun displayText(text: String, x: Int, y: Int, brightness: Int = 255, mode: GlyphMode = GlyphMode.APP) {
        glyphDataSource.displayText(
            text = text,
            x = x,
            y = y,
            brightness = brightness,
            mode = mode
        )
    }

    fun clearDisplay(mode: GlyphMode = GlyphMode.APP) {
        glyphDataSource.clearDisplay(mode)
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
