package xyz.peatral.blinkr.feature.timer.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.di.ApplicationScope
import xyz.peatral.blinkr.core.domain.AppInitializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerInitializer @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val orchestrateTimerService: OrchestrateTimerServiceUseCase,
    private val persistSessionToStorage: PersistSessionToStorageUseCase,
    private val rescheduleTimer: RescheduleTimerUseCase,
    private val deriveSessionStateFromTimers: DeriveSessionStateUseCase,
) : AppInitializer {

    override fun initialize() {
        appScope.launch {
            launch {
                orchestrateTimerService()
            }
            launch {
                deriveSessionStateFromTimers()
            }
            launch {
                persistSessionToStorage()
            }
            launch {
                rescheduleTimer()
            }
        }
    }
}
