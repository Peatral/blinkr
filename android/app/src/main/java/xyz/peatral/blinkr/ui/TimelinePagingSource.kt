package xyz.peatral.blinkr.ui

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.data.repository.SyncRepository
import kotlin.time.Duration.Companion.days

data class DayRecord(val daysAgo: Int, val sessions: List<SessionEntity>)

class TimelinePagingSource(
    private val syncRepository: SyncRepository
) : PagingSource<Int, DayRecord>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DayRecord> {
        val startDaysAgo = params.key ?: 0
        val loadSize = params.loadSize

        val zone = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(zone).date.atStartOfDayIn(zone)

        val oldestEntryTime = syncRepository.getOldestSessionStartTime() ?: return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )

        val chunkEndDaysAgo = maxOf(0, startDaysAgo - 1)
        val chunkEnd = startOfToday - chunkEndDaysAgo.days + 1.days
        val chunkStart = startOfToday - (startDaysAgo + loadSize - 1).days

        return try {
            // TODO: invalidate source when repo update
            val chunkSessions = syncRepository.getSessionsForTimeframe(chunkStart, chunkEnd).first()

            val days = (0 until loadSize).map { offset ->
                val daysAgo = startDaysAgo + offset
                val dayStart = startOfToday - daysAgo.days
                val dayEnd = dayStart + 1.days

                val dailySessions = chunkSessions.filter {
                    it.endTime > dayStart && it.startTime < dayEnd
                }

                DayRecord(daysAgo, dailySessions)
            }

            val reachedEnd = chunkStart <= oldestEntryTime

            LoadResult.Page(
                data = days,
                prevKey = if (startDaysAgo == 0) null else maxOf(0, startDaysAgo - loadSize),
                nextKey = if (reachedEnd) null else startDaysAgo + loadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DayRecord>): Int? {
        return state.anchorPosition
    }
}