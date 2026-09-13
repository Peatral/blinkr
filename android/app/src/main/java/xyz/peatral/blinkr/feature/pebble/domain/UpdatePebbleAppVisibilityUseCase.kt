package xyz.peatral.blinkr.feature.pebble.domain

import xyz.peatral.blinkr.feature.pebble.data.PebbleRepository
import java.util.UUID
import javax.inject.Inject

class UpdatePebbleAppVisibilityUseCase @Inject constructor(
    private val pebbleRepository: PebbleRepository
) {
    operator fun invoke(uuid: UUID, isVisible: Boolean) {
        pebbleRepository.setAppOpen(uuid, isVisible)
    }
}