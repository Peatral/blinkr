package xyz.peatral.blinkr.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

sealed class TimelineItem {
    data class Session(val start: Instant, val end: Instant, val isActive: Boolean) : TimelineItem()
    data class Break(val start: Instant, val end: Instant) : TimelineItem()
    data object Empty : TimelineItem()
}

fun getTimelineItemsForDate(targetDate: LocalDate, sessions: List<SessionEntity>): List<TimelineItem> {
    val zone = TimeZone.currentSystemDefault()
    val dayStart = targetDate.atStartOfDayIn(zone)
    val dayEnd = dayStart + 1.days
    val currentTime = Clock.System.now()

    val sortedSessions = sessions.sortedBy { it.startTime }
    val items = mutableListOf<TimelineItem>()
    var previousEnd: Instant? = null

    sortedSessions.forEach { session ->
        val actualStart = maxOf(session.startTime, dayStart)

        val effectiveEnd = session.endTime ?: currentTime
        val actualEnd = minOf(effectiveEnd, dayEnd)

        if (previousEnd != null && actualStart > previousEnd) {
            items.add(TimelineItem.Break(start = previousEnd, end = actualStart))
        }

        items.add(TimelineItem.Session(actualStart, actualEnd, isActive = session.endTime == null))

        previousEnd = actualEnd
    }

    if (items.isEmpty()) {
        items.add(TimelineItem.Empty)
    }

    return items
}

@Composable
fun DayBreakdownScreen(
    viewModel: DayBreakdownViewModel,
    onNavigateBack: () -> Unit,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    var titleModifier: Modifier = Modifier

    if (sharedScope != null && animScope != null) {
        with(sharedScope) {
            val dateString = viewModel.targetDate.toString()
            titleModifier = titleModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "title-$dateString"),
                animatedVisibilityScope = animScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    }

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val timelineItems = remember(viewModel.targetDate, sessions) {
        getTimelineItemsForDate(viewModel.targetDate, sessions)
    }

    val title = remember(viewModel.targetDate) {
        viewModel.formatDate(viewModel.targetDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = titleModifier,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    itemsIndexed(timelineItems) { index, item ->
                        TimelineItemRow(
                            item = item,
                            formatDuration = viewModel.formatDuration,
                            index = index,
                            count = timelineItems.size,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItemRow(
    item: TimelineItem,
    formatDuration: (duration: Duration) -> String,
    index: Int,
    count: Int,
) {
    when (item) {
        is TimelineItem.Empty -> {
            Text(
                text = stringResource(R.string.no_recorded_sessions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
            )
        }
        is TimelineItem.Session -> {
            TimelineRowLayout(
                title = stringResource(R.string.session),
                start = item.start,
                end = item.end,
                isActive = item.isActive,
                formatDuration = formatDuration,
                index = index,
                count = count,
            )
        }
        is TimelineItem.Break -> {
            TimelineRowLayout(
                title = stringResource(R.string.session_break),
                start = item.start,
                end = item.end,
                isActive = false,
                formatDuration = formatDuration,
                index = index,
                count = count,
            )
        }
    }
}

@Composable
private fun TimelineRowLayout(
    title: String,
    start: Instant,
    end: Instant,
    isActive: Boolean,
    formatDuration: (duration: Duration) -> String,
    index: Int,
    count: Int,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }

    val startTimeStr = timeFormatter.format(java.time.Instant.ofEpochMilli(start.toEpochMilliseconds()))
    val endTimeStr = if (isActive) "--:--" else timeFormatter.format(java.time.Instant.ofEpochMilli(end.toEpochMilliseconds()))
    val timeRangeText = "$startTimeStr - $endTimeStr"

    val durationText = formatDuration(end - start)

    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        },
        trailingContent = {
            Text(
                text = durationText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                softWrap = false,
            )
        },
        supportingContent = {
            Text(
                text = timeRangeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                softWrap = false,
            )
        },
    )
}
