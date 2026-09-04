package xyz.peatral.blinkr.data.room

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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val startTime: Instant,
    val endTime: Instant
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(sessions: SessionEntity)

    @Query("""
        DELETE FROM sessions 
        WHERE startTime = (SELECT startTime FROM sessions ORDER BY startTime DESC LIMIT 1) 
        AND endTime = :distantFuture
    """)
    suspend fun deleteLatestSessionIfUnfinished(distantFuture: Instant)

    suspend fun deleteUnfinishedSession() {
        deleteLatestSessionIfUnfinished(Instant.DISTANT_FUTURE)
    }

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessionsDesc(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime ASC")
    suspend fun getAllSessionsAsc(): List<SessionEntity>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Transaction
    suspend fun revalidateData(currentTime: Instant) {
        val history = getAllSessionsAsc()
        if (history.isEmpty()) return

        val maxAllowedTime = currentTime + 1.days

        val validHistory = history.filter {
            it.startTime.toEpochMilliseconds() > 0 &&
                    it.endTime >= it.startTime &&
                    it.endTime <= maxAllowedTime
        }

        val mergedHistory = mutableListOf<SessionEntity>()
        for (current in validHistory) {
            val last = mergedHistory.lastOrNull()

            if (last != null && current.startTime < (last.endTime + 1.minutes)) {
                if (current.endTime > last.endTime) {
                    mergedHistory[mergedHistory.lastIndex] = last.copy(endTime = current.endTime)
                }
            } else {
                mergedHistory.add(current)
            }
        }

        val finalHistory = mergedHistory.filter {
            (it.endTime - it.startTime) >= 1.minutes
        }

        deleteAll()
        insertAll(finalHistory)
    }

    @Query("SELECT * FROM sessions WHERE endTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    fun getSessionsForTimeframe(startOfDay: Instant, endOfDay: Instant): Flow<List<SessionEntity>>
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