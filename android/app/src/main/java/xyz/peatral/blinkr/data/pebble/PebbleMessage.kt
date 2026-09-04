package xyz.peatral.blinkr.data.pebble

import kotlin.time.Instant

sealed class PebbleMessage {
    data class RescheduleTimer(val next_wakeup: Instant) : PebbleMessage()
    data class StartSession(val startTimestamp: Instant) : PebbleMessage()
    data class StopSession(val startTimestamp: Instant, val endTimestamp: Instant) : PebbleMessage()

    data class SyncStart(val totalChunks: Int) : PebbleMessage()
    data class SyncChunk(val data: ByteArray) : PebbleMessage()
}