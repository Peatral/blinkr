package xyz.peatral.blinkr.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import xyz.peatral.blinkr.data.repository.TimerRepository
import xyz.peatral.blinkr.di.DefaultDispatcher
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FormatTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val currentTimeUseCase: CurrentTimeUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<String> = timerRepository.timer
        .flatMapLatest { timer ->
            if (timer == null) {
                flowOf("")
            } else {
                currentTimeUseCase(1.seconds, timer.end)
                    .map { currentTime -> (timer.end - currentTime)
                        .toComponents { minutes, seconds, _ ->
                            String.format("%02d:%02d", minutes, seconds)
                        }
                    }
            }
        }.flowOn(defaultDispatcher)
}