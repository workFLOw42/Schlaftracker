package de.schlafgut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.ui.theme.Dream400
import de.schlafgut.app.ui.theme.Night700
import de.schlafgut.app.ui.theme.QualityGood
import de.schlafgut.app.ui.theme.QualityMedium
import de.schlafgut.app.ui.theme.QualityPoor
import de.schlafgut.app.ui.theme.TextMuted
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor
import de.schlafgut.app.util.DateTimeUtil

/**
 * Schlafdauer-Trend als Liniendiagramm.
 */
@Composable
fun DurationChart(
    entries: List<SleepEntryEntity>,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "Schlafdauer", modifier = modifier) {
        if (entries.isEmpty()) return@ChartCard

        val sorted = entries.sortedBy { it.bedTime }
        val maxHours = 12f
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMuted)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 20f
            val chartH = h - padBottom

            // Grid lines at 4h, 8h
            listOf(4f, 8f).forEach { hours ->
                val y = chartH - (hours / maxHours) * chartH
                drawLine(Night700, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Line path
            val path = Path()
            sorted.forEachIndexed { i, entry ->
                val x = if (sorted.size == 1) w / 2 else (i.toFloat() / (sorted.size - 1)) * w
                val hours = entry.sleepDurationMinutes / 60f
                val y = chartH - (hours / maxHours).coerceIn(0f, 1f) * chartH

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Dot
                drawCircle(Dream400, 4f, Offset(x, y))
            }
            drawPath(path, Dream400, style = Stroke(width = 2f))

            // Y-axis labels
            listOf("0h", "4h", "8h", "12h").forEachIndexed { i, label ->
                val y = chartH - (i * 4f / maxHours) * chartH
                val result = textMeasurer.measure(label, textStyle)
                drawText(result, topLeft = Offset(0f, y - result.size.height / 2f))
            }
        }
    }
}

/**
 * Schlafqualität als Balkendiagramm.
 */
@Composable
fun QualityChart(
    entries: List<SleepEntryEntity>,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "Schlafqualit\u00E4t", modifier = modifier) {
        if (entries.isEmpty()) return@ChartCard

        val sorted = entries.sortedBy { it.bedTime }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 20f
            val chartH = h - padBottom
            val barWidth = (w / sorted.size.coerceAtLeast(1)).coerceAtMost(40f)
            val gap = if (sorted.size > 1) (w - barWidth * sorted.size) / (sorted.size - 1) else 0f

            // Grid at 5
            val y5 = chartH - (5f / 10f) * chartH
            drawLine(Night700, Offset(0f, y5), Offset(w, y5), strokeWidth = 1f)

            sorted.forEachIndexed { i, entry ->
                val x = i * (barWidth + gap)
                val barH = (entry.quality / 10f) * chartH
                val y = chartH - barH

                drawRect(
                    color = qualityColor(entry.quality),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH)
                )
            }
        }
    }
}

/**
 * Unterbrechungen als Liniendiagramm.
 */
@Composable
fun InterruptionsChart(
    entries: List<SleepEntryEntity>,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "Unterbrechungen", modifier = modifier) {
        if (entries.isEmpty()) return@ChartCard

        val sorted = entries.sortedBy { it.bedTime }
        val maxInterruptions = sorted.maxOf { it.interruptionCount }.coerceAtLeast(3).toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val w = size.width
            val h = size.height

            // Step line
            val path = Path()
            sorted.forEachIndexed { i, entry ->
                val x = if (sorted.size == 1) w / 2 else (i.toFloat() / (sorted.size - 1)) * w
                val y = h - (entry.interruptionCount / maxInterruptions) * h

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(WakeWindowColor, 4f, Offset(x, y))
            }
            drawPath(path, WakeWindowColor, style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
