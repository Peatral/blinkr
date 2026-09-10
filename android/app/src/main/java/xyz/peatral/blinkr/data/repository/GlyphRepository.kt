package xyz.peatral.blinkr.data.repository

import xyz.peatral.blinkr.data.datasource.GlyphDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphRepository @Inject constructor(
    private val glyphDataSource: GlyphDataSource
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

    fun displayText(text: String, x: Int, y: Int) {
        glyphDataSource.displayText(text, x, y)
    }

    fun clearDisplay() {
        glyphDataSource.clearDisplay()
    }
}