package xyz.peatral.blinkr.feature.pebble.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.feature.pebble.data.PebbleConstants
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SyncRepository
import xyz.peatral.blinkr.core.data.repository.SyncState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SyncPebbleDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val pebbleRepository: PebbleRepository,
    private val requestSync: RequestPebbleSyncUseCase,
) {
    suspend operator fun invoke() = coroutineScope {
        var expectedChunks = 0
        var receivedChunks = 0
        val syncBuffer = mutableListOf<SessionEntity>()

        launch {
            pebbleRepository.incomingMessages.collect { message ->
                when (message) {
                    is PebbleMessage.SyncStart -> {
                        expectedChunks = message.totalChunks
                        receivedChunks = 0
                        syncBuffer.clear()
                        syncRepository.updateSyncState(SyncState.Syncing(0f))
                    }

                    is PebbleMessage.SyncChunk -> {
                        val parsedSessions = parseHistoryBytes(message.data)
                        syncBuffer.addAll(parsedSessions)
                        receivedChunks++

                        val progress = (receivedChunks.toFloat() / expectedChunks.toFloat()).coerceIn(0.0f, 1.0f)
                        syncRepository.updateSyncState(SyncState.Syncing(progress))

                        if (expectedChunks in 1..receivedChunks) {
                            syncRepository.saveSessions(syncBuffer)
                            syncBuffer.clear()
                            expectedChunks = 0
                            syncRepository.updateSyncState(SyncState.Idle)
                        }
                    }

                    is PebbleMessage.StartSession -> {
                        syncRepository.saveSession(SessionEntity(message.startTimestamp, endTime = null))
                    }

                    is PebbleMessage.StopSession -> {
                        syncRepository.deleteUnfinishedSession()
                        if (isValidSession(message.startTimestamp, message.endTimestamp)) {
                            syncRepository.saveSession(SessionEntity(message.startTimestamp, message.endTimestamp))
                        }
                    }

                    else -> {}
                }
            }
        }

        launch {
            syncRepository.syncRequests.collect {
                requestSync()
            }
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

    private fun isValidSession(start: Instant, end: Instant): Boolean {
        return start > PebbleConstants.DISTANT_PAST &&
                end < PebbleConstants.DISTANT_FUTURE &&
                start < end &&
                end - start > 1.minutes
    }
}