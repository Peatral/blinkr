package xyz.peatral.blinkr.data.pebble

import kotlin.time.Instant

object PebbleConstants {
    val DISTANT_FUTURE = Instant.fromEpochSeconds(Int.MAX_VALUE.toLong())
    val DISTANT_PAST = Instant.fromEpochSeconds(Int.MIN_VALUE.toLong())
}