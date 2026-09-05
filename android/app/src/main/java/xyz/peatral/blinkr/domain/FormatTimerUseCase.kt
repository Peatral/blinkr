package xyz.peatral.blinkr.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import xyz.peatral.blinkr.data.repository.TimerRepository
import xyz.peatral.blinkr.di.DefaultDispatcher
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FormatTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<String> = timerRepository.nextWakeup
        .flatMapLatest{ nextWakeup ->
            if (nextWakeup == null) {
                flowOf("")
            } else {
                flow {
                    var currentTime = Clock.System.now()
                    while (currentTime <= nextWakeup) {
                        val remaining = nextWakeup - currentTime
                        val formattedDuration = remaining.toComponents { minutes, seconds, _ ->
                            String.format("%02d:%02d", minutes, seconds)
                        }
                        emit(formattedDuration)
                        delay(1.seconds)
                        currentTime = Clock.System.now()
                    }
                }
            }
        }.flowOn(defaultDispatcher)
}