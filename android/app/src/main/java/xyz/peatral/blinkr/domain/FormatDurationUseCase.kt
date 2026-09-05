package xyz.peatral.blinkr.domain

import javax.inject.Inject
import kotlin.time.Duration

class FormatDurationUseCase @Inject constructor() {
    operator fun invoke(duration: Duration): String = duration.toComponents { minutes, seconds, _ ->
        String.format("%02d:%02d", minutes, seconds)
    }
}