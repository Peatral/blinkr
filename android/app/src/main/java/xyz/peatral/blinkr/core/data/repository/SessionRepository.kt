package xyz.peatral.blinkr.core.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import xyz.peatral.blinkr.core.data.AppState
import xyz.peatral.blinkr.core.data.EventOrigin
import xyz.peatral.blinkr.core.data.datasource.room.SessionDao
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
sealed interface SessionState {
    val startTime: Instant

    @Serializable
    data class Break(override val startTime: Instant = Clock.System.now()) : SessionState
    @Serializable
    data class Active(override val startTime: Instant = Clock.System.now()) : SessionState
}

data class SessionStateUpdate(
    val prevState: SessionState,
    val nextState: SessionState,
    val origin: EventOrigin
)

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val appStateDataStore: DataStore<AppState>,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    init {
        appScope.launch {
            val appState = appStateDataStore.data.first()
            _sessionState.value = appState.sessionState
        }
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Break())
    val sessionState = _sessionState.asStateFlow()

    private val _sessionStateUpdates = MutableSharedFlow<SessionStateUpdate>(extraBufferCapacity = 10)
    val sessionStateUpdates = _sessionStateUpdates.asSharedFlow()

    fun updateState(newState: SessionState, origin: EventOrigin = EventOrigin.LOCAL) {
        val oldState = _sessionState.value
        if (oldState == newState) return

        _sessionState.value = newState
        persistStateAsync(newState)

        _sessionStateUpdates.tryEmit(
            SessionStateUpdate(prevState = oldState, nextState = newState, origin = origin)
        )
    }

    private fun persistStateAsync(state: SessionState) {
        appScope.launch {
            appStateDataStore.updateData { currentAppState ->
                currentAppState.copy(
                    sessionState = state
                )
            }
        }
    }

    suspend fun saveSessions(sessions: List<SessionEntity>) {
        sessionDao.insertAll(sessions)
    }

    suspend fun saveSession(session: SessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun deleteAllUnfinishedSessions() {
        sessionDao.deleteAllUnfinishedSessions()
    }

    fun getSessionsForTimeframe(startOfDay: Instant, endOfDay: Instant): Flow<List<SessionEntity>> {
        return sessionDao.getSessionsForTimeframe(startOfDay, endOfDay)
    }

    suspend fun getOldestSessionStartTime(): Instant? {
        return sessionDao.getOldestSessionStartTime()
    }

    fun getSessionUpdatesFlow(): Flow<Int> {
        return sessionDao.getSessionCountFlow()
    }

    suspend fun getLatestSession(): SessionEntity? {
        return sessionDao.getLatestSession()
    }

    suspend fun getAllSessionsAsc(): List<SessionEntity> {
        return sessionDao.getAllSessionsAsc()
    }

    suspend fun replaceHistory(validSessions: List<SessionEntity>, activeSession: SessionEntity?) {
        sessionDao.replaceHistory(validSessions, activeSession)
    }
}