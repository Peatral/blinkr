package xyz.peatral.blinkr.feature.glyph.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphRepository @Inject constructor(
    private val glyphDataSource: GlyphDataSource,
) {
    var references = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun connect() {
        glyphDataSource.connect()
        references++
    }

    fun disconnect() {
        references = 0.coerceAtLeast(references - 1)
        if (references <= 0) {
            clearDisplay()
            scope.launch {
                delay(100)
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
