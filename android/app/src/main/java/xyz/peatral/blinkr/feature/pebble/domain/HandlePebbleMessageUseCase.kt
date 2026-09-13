package xyz.peatral.blinkr.feature.pebble.domain

import io.rebble.pebblekit2.common.model.PebbleDictionary
import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import java.util.UUID
import javax.inject.Inject

class HandlePebbleMessageUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository
) {
    suspend operator fun invoke(uuid: UUID, data: PebbleDictionary): Boolean {
        return pebbleRepository.processIncomingMessage(uuid, data)
    }
}