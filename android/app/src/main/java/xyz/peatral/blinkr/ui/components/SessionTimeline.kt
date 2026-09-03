package xyz.peatral.blinkr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.peatral.blinkr.data.room.SessionEntity

@Composable
fun SessionTimeline(
    sessions: List<SessionEntity>,
    startMillis: Long,
    endMillis: Long,
    currentTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    val pastColor = Color(0xFF4CAF50)
    val sessionColor = MaterialTheme.colorScheme.error
    val futureColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
            .clip(CircleShape)
    ) {
        val width = size.width
        val height = size.height
        val timeframeDuration = endMillis - startMillis

        if (timeframeDuration <= 0) return@Canvas

        drawRect(
            color = futureColor,
            size = Size(width, height)
        )

        val elapsedFraction = ((currentTimeMillis - startMillis).toFloat() / timeframeDuration).coerceIn(0f, 1f)
        val elapsedWidth = width * elapsedFraction

        drawRect(
            color = pastColor,
            size = Size(elapsedWidth, height)
        )

        val effectiveCurrentTime = currentTimeMillis.coerceAtMost(endMillis)

        for (session in sessions) {
            val sessionStartMillis = session.startTime * 1000L
            val sessionEndMillis = session.endTime * 1000L

            if (sessionEndMillis > startMillis && sessionStartMillis < endMillis) {
                val clampedStart = sessionStartMillis.coerceAtLeast(startMillis)

                val clampedEnd = sessionEndMillis.coerceAtMost(effectiveCurrentTime)

                val startFraction = ((clampedStart - startMillis).toFloat() / timeframeDuration).coerceIn(0f, 1f)
                val endFraction = ((clampedEnd - startMillis).toFloat() / timeframeDuration).coerceIn(0f, 1f)

                val startX = width * startFraction
                val sessionWidth = (width * endFraction) - startX

                if (sessionWidth > 0) {
                    drawRect(
                        color = sessionColor,
                        topLeft = Offset(x = startX, y = 0f),
                        size = Size(sessionWidth, height)
                    )
                }
            }
        }
    }
}