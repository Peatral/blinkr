package xyz.peatral.blinkr.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import xyz.peatral.blinkr.di.DefaultDispatcher
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class CurrentTimeUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke() = flow {
        while (true) {
            emit(Clock.System.now())
            delay(1.minutes)
        }
    }.flowOn(defaultDispatcher)
}