package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.peatral.blinkr.core.data.datasource.room.SessionDao
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface SyncState {
    object Idle : SyncState
    class Syncing(progress: Float) : SyncState
}

@Singleton
class SyncRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _syncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncRequests = _syncRequests.asSharedFlow()

    fun requestSync() {
        _syncRequests.tryEmit(Unit)
    }

    fun updateSyncState(state: SyncState) {
        _syncState.value = state
    }

    suspend fun saveSessions(sessions: List<SessionEntity>) {
        sessionDao.insertAll(sessions)
        sessionDao.revalidateData(Clock.System.now())
    }

    suspend fun saveSession(session: SessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun deleteUnfinishedSession() {
        sessionDao.deleteUnfinishedSession()
    }

    suspend fun getUnfinishedSessionStartTime(): Instant? {
        return sessionDao.getUnfinishedSessionStartTime()
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
}