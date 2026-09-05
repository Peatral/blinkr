package xyz.peatral.blinkr.data.datasource.pebble

import kotlin.time.Instant

object PebbleConstants {
    val DISTANT_FUTURE = Instant.fromEpochSeconds(Int.MAX_VALUE.toLong())
    val DISTANT_PAST = Instant.fromEpochSeconds(Int.MIN_VALUE.toLong())
}

object MessageKeys {
    const val MSG_TYPE = 10000u
    const val SYNC_TOTAL_CHUNKS = 10001u
    const val SYNC_DATA_CHUNK = 10002u
    const val NEXT_WAKEUP = 10003u
    const val START_TIMESTAMP = 10004u
    const val END_TIMESTAMP = 10005u
    const val INTERVAL = 10006u
}

object MessageTypes {
    const val TYPE_RESCHEDULE_WAKEUP = 1
    const val TYPE_START_SESSION = 2
    const val TYPE_STOP_SESSION = 3
    const val TYPE_SYNC_START = 4
    const val TYPE_SYNC_CHUNK = 5
    const val TYPE_REQUEST_SYNC = 6
}
