package xyz.peatral.blinkr.core.domain

import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class FormatDurationUseCase @Inject constructor() {
    operator fun invoke(duration: Duration): String {
        return duration.toComponents { hours, minutes, _, _ ->
            if (duration < 1.hours) {
                "${minutes}m"
            } else {
                "${hours}h ${minutes}m"
            }
        }
    }
}