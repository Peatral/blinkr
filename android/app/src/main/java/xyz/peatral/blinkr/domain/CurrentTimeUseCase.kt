package xyz.peatral.blinkr.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import xyz.peatral.blinkr.di.DefaultDispatcher
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class CurrentTimeUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(updateInterval: Duration = 1.minutes, endTime: Instant = Instant.DISTANT_FUTURE) = flow {
        var currentTime = Clock.System.now()
        while (currentTime <= endTime) {
            emit(currentTime)
            delay(updateInterval)
            currentTime = Clock.System.now()
        }
    }.flowOn(defaultDispatcher)
}