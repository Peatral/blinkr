package xyz.peatral.blinkr.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.data.datasource.pebble.PebbleConstants
import xyz.peatral.blinkr.data.datasource.pebble.PebbleDataSource
import xyz.peatral.blinkr.data.datasource.pebble.PebbleMessage
import xyz.peatral.blinkr.data.datasource.room.SessionDao
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

sealed interface SyncState {
    object Idle : SyncState
    class Syncing(progress: Float) : SyncState
}

@Singleton
class SyncRepository @Inject constructor(
    private val pebbleDataSource: PebbleDataSource,
    private val sessionDao: SessionDao
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var expectedChunks = 0
    private var receivedChunks = 0
    private val syncBuffer = mutableListOf<SessionEntity>()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    init {
        syncScope.launch {
            pebbleDataSource.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.SyncStart -> handleSyncStart(message)
                    is PebbleMessage.SyncChunk -> handleSyncChunk(message)
                    is PebbleMessage.StartSession -> handleStartSession(message)
                    is PebbleMessage.StopSession -> handleStopSession(message)
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleStartSession(message: PebbleMessage.StartSession) {
        sessionDao.insertSession(SessionEntity(
            startTime = message.startTimestamp,
            endTime = PebbleConstants.DISTANT_FUTURE,
        ))
    }

    private suspend fun handleStopSession(message: PebbleMessage.StopSession) {
        sessionDao.deleteUnfinishedSession()
        if (
            message.startTimestamp > PebbleConstants.DISTANT_PAST && message.endTimestamp < PebbleConstants.DISTANT_FUTURE
            && message.startTimestamp < message.endTimestamp
            && message.endTimestamp - message.startTimestamp > 1.minutes
        ) {
            sessionDao.insertSession(SessionEntity(
                startTime = message.startTimestamp,
                endTime = message.endTimestamp,
            ))
        }
    }

    fun getSessionsForTimeframe(startOfDay: Instant, endOfDay: Instant): Flow<List<SessionEntity>> {
        return sessionDao.getSessionsForTimeframe(startOfDay, endOfDay)
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


        _syncState.value = SyncState.Syncing((receivedChunks.toFloat() / expectedChunks.toFloat()).coerceIn(0.0f, 1.0f))

        if (expectedChunks in 1..receivedChunks) {
            sessionDao.insertSessions(syncBuffer)
            sessionDao.revalidateData(Clock.System.now())
            syncBuffer.clear()
            expectedChunks = 0
            _syncState.value = SyncState.Idle
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
            pairs.add(SessionEntity(startTime = Instant.fromEpochSeconds(start), endTime = Instant.fromEpochSeconds(end)))
        }
        return pairs
    }

    fun requestSync() {
        _syncState.value = SyncState.Syncing(0.0f)
        syncScope.launch {
            pebbleDataSource.sendMessageToWatch(PebbleMessage.RequestSync)
        }
    }
}