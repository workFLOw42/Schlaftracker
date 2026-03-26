package de.schlafgut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.ui.theme.LatencyColor
import de.schlafgut.app.ui.theme.MedianBedColor
import de.schlafgut.app.ui.theme.MedianWakeColor
import de.schlafgut.app.ui.theme.Night600
import de.schlafgut.app.ui.theme.Night700
import de.schlafgut.app.ui.theme.TextMuted
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor

/**
 * 24h-Timeline (18:00 → 18:00) die alle Einträge als horizontale Balken zeigt.
 * Enthält Median-Linien für Bettzeit und Aufwachzeit.
 */
@Composable
fun SleepTimeline24h(
    entries: List<SleepEntryEntity>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Text(
            text = "Keine Eintr\u00E4ge im gew\u00E4hlten Zeitraum",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMuted)

    // Time labels: 18, 20, 22, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18
    val timeLabels = listOf(
        "18:00", "20:00", "22:00", "00:00", "02:00", "04:00",
        "06:00", "08:00", "10:00", "12:00", "14:00", "16:00", "18:00"
    )

    val barHeight = 20f
    val barSpacing = 6f
    val topPadding = 24f
    val bottomPadding = 24f
    val totalHeight = topPadding + (entries.size * (barHeight + barSpacing)) + bottomPadding

    // Median calculations
    val bedNorms = entries.map { normalizeToAxis(it.bedTime) }
    val wakeNorms = entries.map { normalizeToAxis(it.wakeTime) }
    val medianBed = median(bedNorms)
    val medianWake = median(wakeNorms)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight.dp)
        ) {
            val width = size.width
            val leftPad = 0f

            // Grid lines (every 2 hours)
            for (i in timeLabels.indices) {
                val x = leftPad + (i / 12f) * (width - leftPad)
                drawLine(
                    color = Night700,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }

            // Median bed time line (dashed)
            val medianBedX = leftPad + medianBed * (width - leftPad)
            drawLine(
                color = MedianBedColor,
                start = Offset(medianBedX, 0f),
                end = Offset(medianBedX, size.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Median wake time line (dashed)
            val medianWakeX = leftPad + medianWake * (width - leftPad)
            drawLine(
                color = MedianWakeColor,
                start = Offset(medianWakeX, 0f),
                end = Offset(medianWakeX, size.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Draw each entry as a bar
            entries.forEachIndexed { index, entry ->
                val y = topPadding + index * (barHeight + barSpacing)
                val bedNorm = normalizeToAxis(entry.bedTime)
                val wakeNorm = normalizeToAxis(entry.wakeTime)

                val startX = leftPad + bedNorm * (width - leftPad)
                val endX = leftPad + wakeNorm * (width - leftPad)
                val barWidth = (endX - startX).coerceAtLeast(4f)

                // Latency portion
                val totalMin = (entry.wakeTime - entry.bedTime) / 60_000f
                val latencyFraction = if (totalMin > 0) entry.sleepLatency / totalMin else 0f
                val latencyW = barWidth * latencyFraction

                // Latency bar
                if (latencyW > 0) {
                    drawRect(
                        color = LatencyColor,
                        topLeft = Offset(startX, y),
                        size = Size(latencyW, barHeight)
                    )
                }

                // Sleep bar
                drawRect(
                    color = qualityColor(entry.quality),
                    topLeft = Offset(startX + latencyW, y),
                    size = Size((barWidth - latencyW).coerceAtLeast(2f), barHeight)
                )

                // Wake windows
                entry.wakeWindows.forEach { window ->
                    val wStart = normalizeToAxis(window.start)
                    val wEnd = normalizeToAxis(window.end)
                    val wx = leftPad + wStart * (width - leftPad)
                    val ww = ((wEnd - wStart) * (width - leftPad)).coerceAtLeast(2f)
                    drawRect(
                        color = WakeWindowColor,
                        topLeft = Offset(wx, y),
                        size = Size(ww, barHeight)
                    )
                }
            }
        }

        // Time labels below
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val width = size.width
            for (i in timeLabels.indices) {
                val x = (i / 12f) * width
                val result = textMeasurer.measure(timeLabels[i], textStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(x - result.size.width / 2f, 0f)
                )
            }
        }
    }
}

private fun median(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[mid - 1] + sorted[mid]) / 2f
    } else {
        sorted[mid]
    }
}
