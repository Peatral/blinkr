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
                glyphManager?.register(Glyph.DEVICE_23112)
                isConnected = true

                if (cont.isActive) cont.resume(Unit)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                isConnected = false
            }
        })
    }

    fun displayText(text: String, x: Int, y: Int, brightness: Int = 255) {
        if (!isConnected || text.isBlank()) return

        val textObject = GlyphMatrixObject.Builder()
            .setText(text)
            .setPosition(x, y)
            .setBrightness(brightness.coerceIn(0, 255))
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(textObject)
            .build(context)

        glyphManager?.setAppMatrixFrame(frame.render())
    }

    fun turnOnAll(brightness: Int = 255) {
        if (!isConnected) return

        val fullScreenFrame = IntArray(25 * 25) { brightness.coerceIn(0, 255) }

        glyphManager?.setAppMatrixFrame(fullScreenFrame)
    }

    fun clearDisplay() {
        if (!isConnected) return

        val emptyFrame = IntArray(25 * 25)
        glyphManager?.setAppMatrixFrame(emptyFrame)
    }

    fun disconnect() {
        glyphManager?.closeAppMatrix()
        glyphManager?.unInit()
        glyphManager = null
        isConnected = false
    }
}
