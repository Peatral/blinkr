package xyz.peatral.blinkr.data.pebble

object PebbleKeys {
    const val INTERVAL = 10000u
    const val SYNC_TOTAL_CHUNKS = 10001u
    const val SYNC_DATA_CHUNK = 10002u
    const val TIMESTAMP = 10003u
    const val DURATION = 10004u
    const val MSG_TYPE = 10005u
}

object PacketIds {
    const val TYPE_RESCHEDULE_WAKEUP = 1
    const val TYPE_STOP_SESSION = 2
    const val TYPE_SYNC_START = 3
    const val TYPE_SYNC_CHUNK = 4
    const val TYPE_START_SESSION = 4
}