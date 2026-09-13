package xyz.peatral.blinkr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import xyz.peatral.blinkr.core.domain.AppInitializer
import javax.inject.Inject

@HiltAndroidApp
class BlinkrApplication : Application() {
    @Inject
    lateinit var initializers: Set<@JvmSuppressWildcards AppInitializer>

    override fun onCreate() {
        super.onCreate()
        initializers.forEach { it.initialize() }
    }
}