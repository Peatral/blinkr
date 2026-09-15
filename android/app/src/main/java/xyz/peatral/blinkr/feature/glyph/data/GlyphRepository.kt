package xyz.peatral.blinkr.feature.glyph.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val matrixSize: Int
        get() = glyphDataSource.matrixSize

    private val appRequests = MutableStateFlow<Map<String, LayerRequest>>(emptyMap())
    private val toyRequests = MutableStateFlow<Map<String, LayerRequest>>(emptyMap())

    init {
        val activeAppCommand = appRequests
            .map { requests -> requests.values.maxByOrNull { it.priority }?.command }
            .distinctUntilChanged()

        val activeToyCommand = toyRequests
            .map { requests -> requests.values.maxByOrNull { it.priority }?.command }
            .distinctUntilChanged()

        appScope.launch {
            activeAppCommand.collectLatest { command ->
                if (command != null) {
                    ensureConnected()
                    glyphDataSource.renderCommand(command, GlyphMode.APP)
                } else {
                    glyphDataSource.closeAppMatrix()
                }
            }
        }

        appScope.launch {
            activeToyCommand.collectLatest { command ->
                if (command != null) {
                    ensureConnected()
                    glyphDataSource.renderCommand(command, GlyphMode.TOY)
                } else if (glyphDataSource.isConnected) {
                    glyphDataSource.renderCommand(GlyphRenderCommand.Clear, GlyphMode.TOY)
                }
            }
        }

        appScope.launch {
            combine(activeAppCommand, activeToyCommand) { app, toy ->
                app == null && toy == null
            }.collectLatest { isCompletelyIdle ->
                if (isCompletelyIdle) {
                    delay(100.milliseconds)
                    glyphDataSource.disconnect()
                }
            }
        }
    }

    private suspend fun ensureConnected() {
        if (!glyphDataSource.isConnected) {
            glyphDataSource.connect()
        }
    }

    /**
     * Called by UseCases to draw a frame.
     * Overwrites any existing request with the same [layerId].
     */
    fun updateLayer(
        mode: GlyphMode,
        layerId: String,
        priority: Int,
        command: GlyphRenderCommand
    ) {
        val request = LayerRequest(priority, command)
        when (mode) {
            GlyphMode.APP -> appRequests.value += (layerId to request)
            GlyphMode.TOY -> toyRequests.value += (layerId to request)
        }
    }

    /**
     * Called by UseCases to remove themselves from the display pool.
     */
    fun removeLayer(mode: GlyphMode, layerId: String) {
        when (mode) {
            GlyphMode.APP -> appRequests.value -= layerId
            GlyphMode.TOY -> toyRequests.value -= layerId
        }
    }

    private data class LayerRequest(
        val priority: Int,
        val command: GlyphRenderCommand
    )
}