package xyz.peatral.blinkr.core.data.datasource.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val startTime: Instant,
    val endTime: Instant? = null
) {
    val isActive: Boolean
        get() = endTime == null
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessions: SessionEntity)

    @Query("""
        DELETE FROM sessions 
        WHERE startTime = (SELECT startTime FROM sessions ORDER BY startTime DESC LIMIT 1) 
        AND endTime IS NULL
    """)
    suspend fun deleteUnfinishedSession()

    @Query("""
        SELECT startTime FROM sessions 
        WHERE startTime = (SELECT startTime FROM sessions ORDER BY startTime DESC LIMIT 1) 
        AND endTime IS NULL
    """)
    suspend fun getUnfinishedSessionStartTime(): Instant?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessionsDesc(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime ASC")
    suspend fun getAllSessionsAsc(): List<SessionEntity>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceHistory(validSessions: List<SessionEntity>, activeSession: SessionEntity?) {
        deleteAll()
        insertAll(validSessions)
        if (activeSession != null) {
            insert(activeSession)
        }
    }

    @Query("""
        SELECT * FROM sessions 
        WHERE (endTime IS NULL OR endTime >= :startOfDay) 
        AND startTime < :endOfDay 
        ORDER BY startTime ASC
    """)
    fun getSessionsForTimeframe(startOfDay: Instant, endOfDay: Instant): Flow<List<SessionEntity>>

    @Query("SELECT MIN(startTime) FROM sessions")
    suspend fun getOldestSessionStartTime(): Instant?

    @Query("SELECT COUNT(startTime) FROM sessions")
    fun getSessionCountFlow(): Flow<Int>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?
}

@Database(
    entities = [SessionEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}