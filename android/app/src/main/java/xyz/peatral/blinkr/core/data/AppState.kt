package xyz.peatral.blinkr.core.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import xyz.peatral.blinkr.core.data.repository.SessionState
import xyz.peatral.blinkr.core.data.repository.TimerState
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppState(
    val sessionState: SessionState = SessionState.Break(),
    val timerState: TimerState = TimerState.Idle(),
)

object AppStateSerializer : Serializer<AppState> {
    override val defaultValue: AppState = AppState()

    override suspend fun readFrom(input: InputStream): AppState {
        return try {
            Json.decodeFromString(
                deserializer = AppState.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(
            Json.encodeToString(serializer = AppState.serializer(), value = t).encodeToByteArray()
        )
    }
}