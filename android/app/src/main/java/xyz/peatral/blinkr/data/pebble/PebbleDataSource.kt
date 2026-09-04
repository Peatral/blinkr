package xyz.peatral.blinkr.data.pebble

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PebbleDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appUuid: UUID = UUID.fromString("dabb3617-783b-443f-8add-8d74ccc57d07")

    private val _incomingMessages = MutableSharedFlow<PebbleMessage>(
        extraBufferCapacity = 10
    )
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val sender = DefaultPebbleSender(context)

    private val channel = PebbleNetworkChannel().apply {

        registerMessage(
            messageClass = PebbleMessage.RescheduleTimer::class,
            messageId = PacketIds.TYPE_RESCHEDULE_WAKEUP,
            encoder = { msg ->
                mapOf(
                    PebbleKeys.TIMESTAMP to PebbleDictionaryItem.Int32(msg.timestamp.epochSeconds.toInt())
                )
            },
            decoder = { dict ->
                val timestamp = dict.getLong(PebbleKeys.TIMESTAMP) ?: return@registerMessage null
                PebbleMessage.RescheduleTimer(Instant.fromEpochSeconds(timestamp))
            }
        )

        registerMessage(
            messageClass = PebbleMessage.StartSession::class,
            messageId = PacketIds.TYPE_START_SESSION,
            encoder = { msg ->
                mapOf(
                    PebbleKeys.TIMESTAMP to PebbleDictionaryItem.Int32(msg.timestamp.epochSeconds.toInt())
                )
            },
            decoder = { dict ->
                val timestamp = dict.getLong(PebbleKeys.TIMESTAMP) ?: return@registerMessage null
                PebbleMessage.StartSession(Instant.fromEpochSeconds(timestamp))
            }
        )

        registerMessage(
            messageClass = PebbleMessage.StopSession::class,
            messageId = PacketIds.TYPE_STOP_SESSION,
            encoder = { msg ->
                mapOf(
                    PebbleKeys.TIMESTAMP to PebbleDictionaryItem.Int32(msg.timestamp.epochSeconds.toInt())
                )
            },
            decoder = { dict ->
                val timestamp = dict.getLong(PebbleKeys.TIMESTAMP) ?: return@registerMessage null
                PebbleMessage.StopSession(Instant.fromEpochSeconds(timestamp))
            }
        )

        registerMessage(
            messageClass = PebbleMessage.SyncStart::class,
            messageId = PacketIds.TYPE_SYNC_START,
            encoder = null,
            decoder = { dict ->
                val total = dict.getInt(PebbleKeys.SYNC_TOTAL_CHUNKS) ?: return@registerMessage null
                PebbleMessage.SyncStart(total)
            }
        )

        registerMessage(
            messageClass = PebbleMessage.SyncChunk::class,
            messageId = PacketIds.TYPE_SYNC_CHUNK,
            encoder = null,
            decoder = { dict ->
                val bytes = dict.getBytes(PebbleKeys.SYNC_DATA_CHUNK) ?: return@registerMessage null
                PebbleMessage.SyncChunk(bytes)
            }
        )
    }

    suspend fun processIncomingMessage(data: PebbleDictionary) {
        val message = channel.decode(data)
        if (message != null) {
            _incomingMessages.emit(message)
        }
    }

    suspend fun sendMessageToWatch(message: PebbleMessage) {
        val payload = channel.encode(message)
        sender.sendDataToPebble(appUuid, payload)
    }

    fun cleanup() {
        sender.close()
    }
}