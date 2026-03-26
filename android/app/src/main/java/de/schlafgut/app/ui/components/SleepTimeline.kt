package de.schlafgut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.ui.theme.LatencyColor
import de.schlafgut.app.ui.theme.Night700
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor

/**
 * Inline-Timeline eines einzelnen Schlafeintrags.
 * Zeigt: [Latenz(grau)][Schlaf(qualitätsfarbe)][Wachphasen(orange overlays)]
 */
@Composable
fun SleepTimeline(
    entry: SleepEntryEntity,
    modifier: Modifier = Modifier,
    barHeight: Dp = 12.dp
) {
    val totalMinutes = ((entry.wakeTime - entry.bedTime) / 60_000f)
    if (totalMinutes <= 0) return

    val qualColor = qualityColor(entry.quality)
    val cornerRadius = CornerRadius(6f, 6f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val width = size.width
        val height = size.height

        // Background track
        drawRoundRect(
            color = Night700,
            size = Size(width, height),
            cornerRadius = cornerRadius
        )

        // Latency segment
        val latencyFraction = entry.sleepLatency / totalMinutes
        val latencyWidth = width * latencyFraction
        if (latencyWidth > 0) {
            drawRoundRect(
                color = LatencyColor,
                size = Size(latencyWidth.coerceAtMost(width), height),
                cornerRadius = cornerRadius
            )
        }

        // Sleep segment (after latency, before end)
        val sleepStart = latencyWidth
        val sleepWidth = width - sleepStart
        if (sleepWidth > 0) {
            drawRoundRect(
                color = qualColor,
                topLeft = Offset(sleepStart, 0f),
                size = Size(sleepWidth, height),
                cornerRadius = cornerRadius
            )
        }

        // Wake window overlays
        entry.wakeWindows.forEach { window ->
            val windowStartMin = (window.start - entry.bedTime) / 60_000f
            val windowEndMin = (window.end - entry.bedTime) / 60_000f
            val x = (windowStartMin / totalMinutes) * width
            val w = ((windowEndMin - windowStartMin) / totalMinutes) * width

            drawRect(
                color = WakeWindowColor,
                topLeft = Offset(x.coerceAtLeast(0f), 0f),
                size = Size(w.coerceAtLeast(2f), height)
            )
        }
    }
}

/**
 * Normalisiert eine Epoch-Zeit auf eine 18:00-18:00 Achse.
 * Gibt einen Wert von 0.0 (18:00) bis 1.0 (18:00 nächster Tag) zurück.
 */
fun normalizeToAxis(epochMillis: Long): Float {
    val dt = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val hour = dt.hour
    val minute = dt.minute

    val minutesFrom18 = if (hour >= 18) {
        (hour - 18) * 60 + minute
    } else {
        (hour + 6) * 60 + minute // +6 because 24-18=6
    }

    return minutesFrom18 / (24f * 60f)
}
