package xyz.peatral.blinkr.data.pebble

sealed class PebbleMessage {
    data class RescheduleTimer(val timestamp: Long) : PebbleMessage()
    data class StopSession(val timestamp: Long) : PebbleMessage()

    data class SyncStart(val totalChunks: Int) : PebbleMessage()
    data class SyncChunk(val data: ByteArray) : PebbleMessage()
}