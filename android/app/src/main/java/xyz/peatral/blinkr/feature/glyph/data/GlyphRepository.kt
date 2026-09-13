package xyz.peatral.blinkr.feature.glyph.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    fun connect() {
        glyphDataSource.connect()
        references++
    }

    fun disconnect() {
        references = 0.coerceAtLeast(references - 1)
        if (references <= 0) {
            clearDisplay()
            appScope.launch {
                delay(100.milliseconds)
                if (references <= 0) {
                    glyphDataSource.disconnect()
                }
            }
        }
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
}
