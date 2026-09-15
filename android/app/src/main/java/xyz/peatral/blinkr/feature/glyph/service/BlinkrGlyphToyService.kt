package xyz.peatral.blinkr.feature.glyph.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.GlyphToy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.feature.glyph.data.GlyphMode
import xyz.peatral.blinkr.feature.glyph.domain.UpdateGlyphDisplayUseCase
import xyz.peatral.blinkr.core.domain.ToggleTimerUseCase
import javax.inject.Inject

@AndroidEntryPoint
class BlinkrGlyphToyService : Service() {

    @Inject lateinit var toggleTimerUseCase: ToggleTimerUseCase
    @Inject lateinit var updateGlyphDisplayUseCase: UpdateGlyphDisplayUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var displayJob: Job? = null

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    if (GlyphToy.EVENT_CHANGE == event) {
                        scope.launch { toggleTimerUseCase() }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent): IBinder? {
        displayJob?.cancel()
        displayJob = scope.launch {
            updateGlyphDisplayUseCase(mode = GlyphMode.TOY)
        }
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        displayJob?.cancel()
        displayJob = null
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
