package xyz.peatral.blinkr.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.data.datasource.room.SessionEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


@Composable
fun DayItem (
    daysAgo: Int,
    sessions: List<SessionEntity>,
    currentTime: Instant,
    daysCount: Int,
    onClick: () -> Unit,
    durationFormatter: (duration: Duration) -> String,
    dateFormatter: (date: LocalDate) -> String,
) {
    val title = remember(daysAgo, dateFormatter) {
        val zone = TimeZone.currentSystemDefault()
        val date = (Clock.System.now() - daysAgo.days).toLocalDateTime(zone).date
        dateFormatter(date)
    }

    val (startTime, endTime) = remember(daysAgo, currentTime) {
        val zone = TimeZone.currentSystemDefault()
        val todayStart = currentTime.toLocalDateTime(zone)
            .date
            .atStartOfDayIn(zone)

        val start = todayStart - daysAgo.days
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
        shapes = ListItemDefaults.segmentedShapes(index = daysAgo, count = daysCount),
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
