package xyz.peatral.blinkr.data.pebble

import kotlin.time.Instant

sealed class PebbleMessage {
    data class RescheduleTimer(val timestamp: Instant) : PebbleMessage()
    data class StartSession(val timestamp: Instant) : PebbleMessage()
    data class StopSession(val timestamp: Instant) : PebbleMessage()

    data class SyncStart(val totalChunks: Int) : PebbleMessage()
    data class SyncChunk(val data: ByteArray) : PebbleMessage()
}