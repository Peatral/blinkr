package xyz.peatral.blinkr.feature.glyph.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import xyz.peatral.blinkr.core.domain.AppInitializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphInitializer @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val glyphTimerWakeLock: GlyphTimerWakeLockUseCase
) : AppInitializer {

    override fun initialize() {
        appScope.launch {
            launch { glyphTimerWakeLock() }
        }
    }
}