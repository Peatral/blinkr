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
    private var isConnected = false

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

    private fun renderFrame(frame: GlyphMatrixFrame, mode: GlyphMode) {
        if (mode == GlyphMode.TOY) {
            glyphManager?.setMatrixFrame(frame.render())
        } else {
            glyphManager?.setAppMatrixFrame(frame.render())
        }
    }

    private fun renderRaw(frame: IntArray, mode: GlyphMode) {
        if (mode == GlyphMode.TOY) {
            glyphManager?.setMatrixFrame(frame)
        } else {
            glyphManager?.setAppMatrixFrame(frame)
        }
    }

    fun displayText(text: String, x: Int, y: Int, brightness: Int = 255, mode: GlyphMode = GlyphMode.APP) {
        if (!isConnected || text.isBlank()) return

        val textObject = GlyphMatrixObject.Builder()
            .setText(text)
            .setPosition(x, y)
            .setBrightness(brightness.coerceIn(0, 255))
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(textObject)
            .build(context)

        renderFrame(frame, mode)
    }

    fun turnOnAll(brightness: Int = 255, mode: GlyphMode = GlyphMode.APP) {
        if (!isConnected) return

        val matrixLength = Common.getDeviceMatrixLength()
        val fullScreenFrame = IntArray(matrixLength * matrixLength) { brightness.coerceIn(0, 255) }

        renderRaw(fullScreenFrame, mode)
    }

    fun clearDisplay(mode: GlyphMode = GlyphMode.APP) {
        if (!isConnected) return

        val matrixLength = Common.getDeviceMatrixLength()
        val emptyFrame = IntArray(matrixLength * matrixLength)
        renderRaw(emptyFrame, mode)
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
}
