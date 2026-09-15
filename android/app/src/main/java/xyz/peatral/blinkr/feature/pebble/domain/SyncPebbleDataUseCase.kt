package xyz.peatral.blinkr.feature.pebble.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SessionRepository
import xyz.peatral.blinkr.core.data.repository.SyncRepository
import xyz.peatral.blinkr.core.data.repository.SyncState
import xyz.peatral.blinkr.core.domain.ReconcileSessionsUseCase
import xyz.peatral.blinkr.feature.pebble.data.PebbleMessage
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

class SyncPebbleDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val sessionRepository: SessionRepository,
    private val pebbleRepository: PebbleRepository,
    private val reconcileSession: ReconcileSessionsUseCase,
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
                            sessionRepository.saveSessions(syncBuffer)

                            reconcileSession(Clock.System.now())

                            syncBuffer.clear()
                            expectedChunks = 0
                            syncRepository.updateSyncState(SyncState.Idle)
                        }
                    }
                    else -> {}
                }
            }
        }

        launch {
            syncRepository.syncRequests.collect {
                syncRepository.updateSyncState(SyncState.Syncing(0.0f))
                pebbleRepository.sendMessageToWatch(PebbleMessage.RequestSync)
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
}