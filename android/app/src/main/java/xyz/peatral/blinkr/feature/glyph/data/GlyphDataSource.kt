package xyz.peatral.blinkr.feature.glyph.data

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume


@Singleton
class GlyphDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var glyphManager: GlyphMatrixManager? = null
    var isConnected = false
        private set
    val matrixSize: Int
        get() = Common.getDeviceMatrixLength()

    suspend fun connect() = suspendCancellableCoroutine { cont ->
        if (isConnected) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        glyphManager = GlyphMatrixManager.getInstance(context)
        glyphManager?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                val device = if (Common.getDeviceMatrixLength() == 13) Glyph.DEVICE_25111p else Glyph.DEVICE_23112
                glyphManager?.register(device)
                isConnected = true

                if (cont.isActive) cont.resume(Unit)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                isConnected = false
            }
        })
    }

    private fun renderRaw(frame: IntArray, mode: GlyphMode) {
        if (mode == GlyphMode.TOY) {
            glyphManager?.setMatrixFrame(frame)
        } else {
            glyphManager?.setAppMatrixFrame(frame)
        }
    }

    fun renderCommand(command: GlyphRenderCommand, mode: GlyphMode) {
        if (!isConnected) return

        when (command) {
            is GlyphRenderCommand.Clear -> {
                renderRaw(IntArray(matrixSize * matrixSize), mode)
            }
            is GlyphRenderCommand.AllOn -> {
                renderRaw(IntArray(matrixSize * matrixSize) { command.brightness }, mode)
            }
            is GlyphRenderCommand.RawFrame -> {
                renderRaw(command.pixels, mode)
            }
            is GlyphRenderCommand.Text -> {
                if (command.text.isBlank()) return

                val actualX = command.x ?: calculateCenteredX(command.text, matrixSize)
                val actualY = command.y ?: calculateCenteredY(matrixSize)

                val textObject = GlyphMatrixObject.Builder()
                    .setText(command.text)
                    .setPosition(actualX, actualY)
                    .setBrightness(command.brightness.coerceIn(0, 255))
                    .build()

                val frame = GlyphMatrixFrame.Builder()
                    .addTop(textObject)
                    .build(context)

                if (mode == GlyphMode.TOY) {
                    glyphManager?.setMatrixFrame(frame.render())
                } else {
                    glyphManager?.setAppMatrixFrame(frame.render())
                }
            }
        }
    }

    fun closeAppMatrix() {
        glyphManager?.closeAppMatrix()
    }

    fun disconnect() {
        glyphManager?.closeAppMatrix()
        glyphManager?.unInit()
        glyphManager = null
        isConnected = false
    }

    /**
     * Dynamically calculates text width based on character types.
     * Matches standard Nothing LED matrix font spacing.
     */
    private fun calculateCenteredX(text: String, matrixSize: Int): Int {
        var widthInPixels = 0

        for (char in text) {
            widthInPixels += when (char) {
                ':', '.', ' ' -> 1
                else -> 4
            }
        }

        if (text.isNotEmpty()) {
            widthInPixels += (text.length - 1)
        }

        return (matrixSize - widthInPixels) / 2
    }

    private fun calculateCenteredY(matrixSize: Int): Int {
        val approxTextHeight = 5
        return (matrixSize - approxTextHeight) / 2
    }
}
