package xyz.peatral.blinkr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.peatral.blinkr.data.room.SessionEntity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun SessionTimeline(
    sessions: List<SessionEntity>,
    startTime: Instant,
    endTime: Instant,
    currentTime: Instant,
    modifier: Modifier = Modifier,
    pastColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    futureColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    sessionColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
            .clip(CircleShape)
    ) {
        val width = size.width
        val height = size.height
        val timeframeDuration: Duration = endTime - startTime

        if (timeframeDuration <= 0.seconds) return@Canvas

        drawRect(
            color = futureColor,
            size = Size(width, height)
        )

        val elapsedTime = currentTime.coerceIn(startTime, endTime) - startTime
        val elapsedFraction = (elapsedTime / timeframeDuration).coerceIn(0.0, 1.0).toFloat()
        val elapsedWidth = width * elapsedFraction

        drawRoundRect(
            color = pastColor,
            size = Size(elapsedWidth, height),
            cornerRadius = CornerRadius(12f, 12f)
        )

        val effectiveCurrentTime = currentTime.coerceAtMost(endTime)

        for (session in sessions) {
            val sessionStart = session.startTime
            val sessionEnd = session.endTime

            if (sessionEnd > startTime && sessionStart < endTime) {
                val clampedStart = sessionStart.coerceAtLeast(startTime)

                val clampedEnd = sessionEnd.coerceAtMost(effectiveCurrentTime)

                val startFraction = ((clampedStart - startTime) / timeframeDuration).coerceIn(0.0, 1.0).toFloat()
                val endFraction = ((clampedEnd - startTime) / timeframeDuration).coerceIn(0.0, 1.0).toFloat()

                val startX = width * startFraction
                val sessionWidth = (width * endFraction) - startX

                if (sessionWidth > 0) {
                    drawRoundRect(
                        color = sessionColor,
                        topLeft = Offset(x = startX, y = 0f),
                        size = Size(sessionWidth, height),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
    }
}