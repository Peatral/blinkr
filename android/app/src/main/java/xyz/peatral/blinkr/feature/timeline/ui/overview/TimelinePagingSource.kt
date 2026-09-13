package xyz.peatral.blinkr.feature.timeline.ui.overview

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.data.repository.SyncRepository
import kotlin.time.Duration.Companion.days

data class DayRecord(val daysAgo: Int, val sessions: List<SessionEntity>)

class TimelinePagingSource(
    private val syncRepository: SyncRepository
) : PagingSource<Int, DayRecord>() {

    private val observerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        observerScope.launch {
            syncRepository.getSessionUpdatesFlow()
                .drop(1)
                .collect {
                    invalidate()
                }
        }

        registerInvalidatedCallback {
            observerScope.cancel()
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DayRecord> {
        val startDaysAgo = when (params) {
            is LoadParams.Refresh -> {
                val key = params.key ?: 0
                maxOf(0, key - (params.loadSize / 2))
            }
            is LoadParams.Append -> params.key ?: 0
            is LoadParams.Prepend -> {
                maxOf(0, params.key - params.loadSize)
            }
        }

        val loadSize = params.loadSize

        val actualLoadSize = if (params is LoadParams.Prepend) {
            params.key - startDaysAgo
        } else {
            params.loadSize
        }

        val zone = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(zone).date.atStartOfDayIn(zone)

        val oldestEntryTime = syncRepository.getOldestSessionStartTime() ?: return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )

        val chunkEndDaysAgo = maxOf(0, startDaysAgo - 1)
        val chunkEnd = startOfToday - chunkEndDaysAgo.days + 1.days
        val chunkStart = startOfToday - (startDaysAgo + actualLoadSize - 1).days

        return try {
            val chunkSessions = syncRepository.getSessionsForTimeframe(chunkStart, chunkEnd).first()

            val days = (0 until loadSize).map { offset ->
                val daysAgo = startDaysAgo + offset
                val dayStart = startOfToday - daysAgo.days
                val dayEnd = dayStart + 1.days

                val dailySessions = chunkSessions.filter {
                    val effectiveEnd = it.endTime ?: Clock.System.now()
                    effectiveEnd > dayStart && it.startTime < dayEnd
                }

                DayRecord(daysAgo, dailySessions)
            }

            val reachedEnd = chunkStart <= oldestEntryTime

            LoadResult.Page(
                data = days,
                prevKey = if (startDaysAgo == 0) null else startDaysAgo,
                nextKey = if (reachedEnd) null else startDaysAgo + actualLoadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DayRecord>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.daysAgo
        }
    }
}