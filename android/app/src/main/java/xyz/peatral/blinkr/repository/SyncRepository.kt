package xyz.peatral.blinkr.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.pebble.PebbleMessage
import xyz.peatral.blinkr.data.room.SessionDao
import xyz.peatral.blinkr.data.room.SessionEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val pebbleDataSource: PebbleDataSource,
    private val sessionDao: SessionDao
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var expectedChunks = 0
    private var receivedChunks = 0
    private val syncBuffer = mutableListOf<SessionEntity>()

    init {
        syncScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.SyncStart -> handleSyncStart(message)
                    is PebbleMessage.SyncChunk -> handleSyncChunk(message)
                    else -> {}
                }
            }
        }
    }

    fun getTodaySessions(): Flow<List<SessionEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfDaySeconds = calendar.timeInMillis / 1000L
        val endOfDaySeconds = startOfDaySeconds + (24 * 60 * 60)

        return sessionDao.getSessionsForTimeframe(startOfDaySeconds, endOfDaySeconds)
    }

    private fun handleSyncStart(message: PebbleMessage.SyncStart) {
        expectedChunks = message.totalChunks
        receivedChunks = 0
        syncBuffer.clear()
    }

    private suspend fun handleSyncChunk(message: PebbleMessage.SyncChunk) {
        val pairs = parseHistoryBytes(message.data)
        syncBuffer.addAll(pairs)
        receivedChunks++

        if (expectedChunks in 1..receivedChunks) {
            sessionDao.insertSessions(syncBuffer)
            syncBuffer.clear()
            expectedChunks = 0
        }
    }

    /**
     * Parses the raw byte array from the Pebble C app.
     * Each TimePair is two 32-bit integers (8 bytes total) in Little Endian.
     */
    private fun parseHistoryBytes(bytes: ByteArray): List<SessionEntity> {
        val pairs = mutableListOf<SessionEntity>()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        while (buffer.remaining() >= 8) {
            val start = buffer.getInt().toLong()
            val end = buffer.getInt().toLong()
            pairs.add(SessionEntity(startTime = start, endTime = end))
        }
        return pairs
    }
}