package xyz.peatral.blinkr.data.datasource

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var glyphManager: GlyphMatrixManager? = null
    private var isConnected = false

    fun connect() {
        if (glyphManager != null) return

        glyphManager = GlyphMatrixManager.getInstance(context)
        glyphManager?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                glyphManager?.register(Glyph.DEVICE_23112)
                isConnected = true
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                isConnected = false
            }
        })
    }

    fun displayText(text: String, x: Int, y: Int) {
        if (!isConnected || text.isBlank()) return

        val textObject = GlyphMatrixObject.Builder()
            .setText(text)
            .setPosition(x, y)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(textObject)
            .build(context)

        glyphManager?.setAppMatrixFrame(frame.render())
    }

    fun clearDisplay() {
        glyphManager?.closeAppMatrix()
    }

    fun disconnect() {
        clearDisplay()
        glyphManager?.unInit()
        glyphManager = null
        isConnected = false
    }
}