package xyz.peatral.blinkr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
fun DayCard(
    daysAgo: Int,
    sessions: List<SessionEntity>,
    currentTime: Instant
) {
    val title = when (daysAgo) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.yesterday)
        else -> stringResource(R.string.n_days_ago, daysAgo)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .padding(0.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = totalDurationText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SessionTimeline(
                sessions = sessions,
                startTime = startTime,
                endTime = endTime,
                currentTime = currentTime,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
