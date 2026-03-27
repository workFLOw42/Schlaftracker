package de.schlafgut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import de.schlafgut.app.ui.theme.TextMuted
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor

/**
 * Schlafdauer-Trend als Liniendiagramm.
 * Punkte sind größer und farblich an die Schlafqualität angepasst.
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
            val padBottom = 24f
            val padLeft = 40f
            val chartH = h - padBottom
            val chartW = w - padLeft

            // Grid lines at 4h, 8h
            listOf(4f, 8f).forEach { hours ->
                val y = chartH - (hours / maxHours) * chartH
                drawLine(Night700, Offset(padLeft, y), Offset(w, y), strokeWidth = 1f)
            }

            // Line path
            val path = Path()
            sorted.forEachIndexed { i, entry ->
                val x = padLeft + if (sorted.size == 1) chartW / 2 else (i.toFloat() / (sorted.size - 1)) * chartW
                val hours = entry.sleepDurationMinutes / 60f
                val y = chartH - (hours / maxHours).coerceIn(0f, 1f) * chartH

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Larger point with quality color
                drawCircle(
                    color = qualityColor(entry.quality),
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
            drawPath(path, Dream400.copy(alpha = 0.5f), style = Stroke(width = 3f))

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
    ChartCard(title = "Schlafqualität (0-10)", modifier = modifier) {
        if (entries.isEmpty()) return@ChartCard

        val sorted = entries.sortedBy { it.bedTime }
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMuted)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val padLeft = 30f
            val chartH = h - padBottom
            val chartW = w - padLeft
            val barWidth = (chartW / sorted.size.coerceAtLeast(1)).coerceAtMost(40f)
            val gap = if (sorted.size > 1) (chartW - barWidth * sorted.size) / (sorted.size - 1) else 0f

            // Grid at 5
            val y5 = chartH - (5f / 10f) * chartH
            drawLine(Night700, Offset(padLeft, y5), Offset(w, y5), strokeWidth = 1f)

            sorted.forEachIndexed { i, entry ->
                val x = padLeft + i * (barWidth + gap)
                val barH = (entry.quality / 10f) * chartH
                val y = chartH - barH

                drawRect(
                    color = qualityColor(entry.quality),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH)
                )
            }

            // Labels
            listOf("0", "5", "10").forEach { label ->
                val valF = label.toFloat()
                val y = chartH - (valF / 10f) * chartH
                val result = textMeasurer.measure(label, textStyle)
                drawText(result, topLeft = Offset(0f, y - result.size.height / 2f))
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
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMuted)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val padLeft = 30f
            val chartH = h - padBottom
            val chartW = w - padLeft

            // Step line
            val path = Path()
            sorted.forEachIndexed { i, entry ->
                val x = padLeft + if (sorted.size == 1) chartW / 2 else (i.toFloat() / (sorted.size - 1)) * chartW
                val y = chartH - (entry.interruptionCount / maxInterruptions) * chartH

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(WakeWindowColor, 4f, Offset(x, y))
            }
            drawPath(path, WakeWindowColor, style = Stroke(width = 2f))

            // Labels
            listOf("0", maxInterruptions.toInt().toString()).forEach { label ->
                val valF = label.toFloat()
                val y = chartH - (valF / maxInterruptions) * chartH
                val result = textMeasurer.measure(label, textStyle)
                drawText(result, topLeft = Offset(0f, y - result.size.height / 2f))
            }
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
