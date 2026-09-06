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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


@Composable
fun DayItem (
    daysAgo: Int,
    sessions: List<SessionEntity>,
    currentTime: Instant,
    daysCount: Int
) {
    val title = when (daysAgo) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.yesterday)
        else -> if (daysAgo < 7) {
            stringResource(R.string.n_days_ago, daysAgo)
        } else {
            // TODO: Display date
            stringResource(R.string.n_days_ago, daysAgo)
        }
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
            val clampedEnd = session.endTime.coerceIn(startTime, minEndTime)
            val clampedStart = session.startTime.coerceIn(startTime, minEndTime)
            (clampedEnd - clampedStart).inWholeMilliseconds
        }.milliseconds
    }

    val totalDurationText = remember(totalDuration) {
        totalDuration.toComponents { hours, minutes, _, _ ->
            if (totalDuration < 1.hours) {
                "${minutes}m"
            } else {
                "${hours}h ${minutes}m"
            }
        }
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
                style = MaterialTheme.typography.titleMedium,
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
        }
    )
}
