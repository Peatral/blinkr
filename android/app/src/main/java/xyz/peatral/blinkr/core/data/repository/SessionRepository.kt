package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.peatral.blinkr.core.data.datasource.room.SessionDao
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    suspend fun startSession(startTime: Instant = Clock.System.now()) {
        sessionDao.insert(SessionEntity(startTime = startTime))
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

    suspend fun getLatestSession(): SessionEntity? {
        return sessionDao.getLatestSession()
    }
}