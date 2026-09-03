package xyz.peatral.blinkr.data

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Common
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

    fun displayTime(timeString: String) {
        if (!isConnected) return

        val matrixSize = Common.getDeviceMatrixLength()

        val approxTextHeight = 5
        val approxTextWidth = 4 * 4 + 4 + 1 // 4 numbers a 4 px, 4 paddings, the colon

        val centerY = (matrixSize - approxTextHeight) / 2
        val centerX = (matrixSize - approxTextWidth) / 2

        val textObject = GlyphMatrixObject.Builder()
            .setText(timeString)
            .setPosition(centerX, centerY)
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