package xyz.peatral.blinkr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.peatral.blinkr.data.room.SessionEntity
import java.util.Calendar

@Composable
fun DayTimeline(
    sessions: List<SessionEntity>,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val pastColor = Color(0xFF4CAF50)
    val sessionColor = MaterialTheme.colorScheme.error
    val futureColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.height(24.dp).fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val cornerRadius = CornerRadius(height / 2, height / 2)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)
        val dayDuration = endOfDay - startOfDay

        drawRoundRect(
            color = futureColor,
            size = Size(width, height),
            cornerRadius = cornerRadius
        )

        val elapsedFraction = ((currentTimeMillis - startOfDay).toFloat() / dayDuration).coerceIn(0f, 1f)
        val elapsedWidth = width * elapsedFraction

        drawRoundRect(
            color = pastColor,
            size = Size(elapsedWidth, height),
            cornerRadius = cornerRadius
        )

        for (session in sessions) {
            val sessionStartMillis = session.startTime * 1000L
            val sessionEndMillis = session.endTime * 1000L

            if (sessionEndMillis > startOfDay && sessionStartMillis < endOfDay) {
                val clampedStart = sessionStartMillis.coerceAtLeast(startOfDay)

                val clampedEnd = sessionEndMillis.coerceAtMost(currentTimeMillis)

                val startFraction = ((clampedStart - startOfDay).toFloat() / dayDuration).coerceIn(0f, 1f)
                val endFraction = ((clampedEnd - startOfDay).toFloat() / dayDuration).coerceIn(0f, 1f)

                val startX = width * startFraction
                val sessionWidth = (width * endFraction) - startX

                if (sessionWidth > 0) {
                    drawRoundRect(
                        color = sessionColor,
                        topLeft = Offset(x = startX, y = 0f),
                        size = Size(sessionWidth, height),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}