package de.schlafgut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
 * Neu: Aufgestanden-Phasen werden innerhalb der Wachphasen dunkler markiert.
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
    val outOfBedColor = Color(0xFFD32F2F) // A darker/distinct red-orange for out of bed

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

            // Sub-segment: Out of bed
            if (window.outOfBed && window.outOfBedStart != null && window.outOfBedEnd != null) {
                val outStartMin = (window.outOfBedStart - entry.bedTime) / 60_000f
                val outEndMin = (window.outOfBedEnd - entry.bedTime) / 60_000f
                val ox = (outStartMin / totalMinutes) * width
                val ow = ((outEndMin - outStartMin) / totalMinutes) * width
                
                drawRect(
                    color = outOfBedColor,
                    topLeft = Offset(ox.coerceAtLeast(0f), height * 0.2f),
                    size = Size(ow.coerceAtLeast(1f), height * 0.6f)
                )
            }
        }
    }
}
