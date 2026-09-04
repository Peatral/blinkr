package xyz.peatral.blinkr.data.pebble

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import kotlin.reflect.KClass

class PebbleNetworkChannel {
    private val decoders = mutableMapOf<Int, (PebbleDictionary) -> PebbleMessage?>()
    private val encoders = mutableMapOf<KClass<*>, (PebbleMessage) -> Map<UInt, PebbleDictionaryItem>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : PebbleMessage> registerMessage(
        messageClass: KClass<T>,
        messageId: Int,
        encoder: ((T) -> Map<UInt, PebbleDictionaryItem>)? = null,
        decoder: ((PebbleDictionary) -> T?)? = null
    ) {
        if (decoder != null) {
            decoders[messageId] = decoder
        }
        if (encoder != null) {
            encoders[messageClass] = { msg ->
                val payload = encoder(msg as T).toMutableMap()
                payload[PebbleKeys.MSG_TYPE] = PebbleDictionaryItem.Int32(messageId)
                payload
            }
        }
    }

    fun decode(dict: PebbleDictionary): PebbleMessage? {
        val msgType = dict.getInt(PebbleKeys.MSG_TYPE) ?: return null
        val decoder = decoders[msgType] ?: return null
        return decoder(dict)
    }

    fun encode(message: PebbleMessage): PebbleDictionary {
        val encoder = encoders[message::class]
            ?: throw UnsupportedOperationException("No encoder registered for ${message::class.simpleName}. (Is it a receive-only message?)")

        return encoder(message)
    }
}