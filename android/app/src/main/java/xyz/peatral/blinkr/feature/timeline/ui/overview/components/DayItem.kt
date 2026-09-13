package xyz.peatral.blinkr.feature.timeline.ui.overview.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import xyz.peatral.blinkr.core.data.datasource.room.SessionEntity
import xyz.peatral.blinkr.core.ui.LocalAnimatedVisibilityScope
import xyz.peatral.blinkr.core.ui.LocalSharedTransitionScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


@Composable
fun DayItem (
    date: LocalDate,
    sessions: List<SessionEntity>,
    currentTime: Instant,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    durationFormatter: (duration: Duration) -> String,
    dateFormatter: (date: LocalDate) -> String,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    var titleModifier: Modifier = Modifier.padding(bottom = 8.dp)

    if (sharedScope != null && animScope != null) {
        with(sharedScope) {
            val dateString = date.toString()
            titleModifier = titleModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "title-$dateString"),
                animatedVisibilityScope = animScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    }

    val title = remember(date, dateFormatter) {
        dateFormatter(date)
    }
    val (startTime, endTime) = remember(date) {
        val zone = TimeZone.currentSystemDefault()
        val start = date.atStartOfDayIn(zone)
        val end = start + 1.days

        start to end
    }

    val totalDuration = remember(startTime, endTime, currentTime, sessions) {
        sessions.sumOf { session ->
            val minEndTime = minOf(endTime, currentTime)
            val effectiveEnd = session.endTime ?: currentTime
            val clampedEnd = effectiveEnd.coerceIn(startTime, minEndTime)
            val clampedStart = session.startTime.coerceIn(startTime, minEndTime)
            (clampedEnd - clampedStart).inWholeMilliseconds
        }.milliseconds
    }

    val totalDurationText = remember(totalDuration, durationFormatter) {
        durationFormatter(totalDuration)
    }

    val textMeasurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium
    val density = LocalDensity.current

    val durationMinWidth = remember(titleStyle, density) {
        with(density) {
            textMeasurer.measure("00h 00m", titleStyle).size.width.toDp()
        }
    }

    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleModifier,
            )
        },
        trailingContent = {
            Text(
                text = totalDurationText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(durationMinWidth),
                textAlign = TextAlign.Right,
                softWrap = false,
            )
        },
        supportingContent = {
            SessionTimeline(
                sessions = sessions,
                startTime = startTime,
                endTime = endTime,
                currentTime = currentTime,
                modifier = Modifier.fillMaxWidth()
            )
        },
        onClick = onClick,
    )
}
