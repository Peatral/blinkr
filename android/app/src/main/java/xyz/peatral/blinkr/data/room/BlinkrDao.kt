package xyz.peatral.blinkr.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val startTime: Long,
    val endTime: Long
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    fun getSessionsForTimeframe(startOfDay: Long, endOfDay: Long): Flow<List<SessionEntity>>
}

@Database(
    entities = [SessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}