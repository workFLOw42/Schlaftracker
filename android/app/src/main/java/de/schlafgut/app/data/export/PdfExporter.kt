package de.schlafgut.app.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.util.DateTimeUtil
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val PAGE_WIDTH = 842  // A4 landscape
    private const val PAGE_HEIGHT = 595

    fun export(
        context: Context,
        entries: List<SleepEntryEntity>,
        startDate: String = "",
        endDate: String = ""
    ): Uri? {
        if (entries.isEmpty()) return null

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 40f
        val contentWidth = PAGE_WIDTH - 2 * margin

        // Paints
        val titlePaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 12f
        }
        val gridPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            strokeWidth = 0.5f
        }
        val medianBedPaint = Paint().apply {
            color = Color.rgb(99, 102, 241)
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val medianWakePaint = Paint().apply {
            color = Color.rgb(20, 184, 166)
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val latencyPaint = Paint().apply { color = Color.rgb(75, 85, 99) }
        val wakePaint = Paint().apply { color = Color.rgb(251, 146, 60) }
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }

        // Background
        canvas.drawColor(Color.rgb(15, 23, 42))

        // Header
        var y = margin + 20f
        canvas.drawText("SchlafGut \u2014 Schlafbericht", margin, y, titlePaint)
        y += 20f
        canvas.drawText("Zeitraum: $startDate \u2013 $endDate", margin, y, subtitlePaint)
        y += 10f

        // Summary
        val avgDuration = entries.map { it.sleepDurationMinutes }.average()
        val avgQuality = entries.map { it.quality }.average()
        val avgInterruptions = entries.map { it.interruptionCount }.average()
        y += 16f
        canvas.drawText(
            "\u00D8 Dauer: ${DateTimeUtil.formatDuration(avgDuration.toInt())}  |  " +
                "\u00D8 Qualit\u00E4t: ${String.format("%.1f", avgQuality)}/10  |  " +
                "\u00D8 Unterbrechungen: ${String.format("%.1f", avgInterruptions)}  |  " +
                "${entries.size} Eintr\u00E4ge",
            margin, y, subtitlePaint
        )

        // Timeline area
        y += 30f
        val timelineTop = y
        val barHeight = 14f
        val barGap = 4f
        val timelineHeight = entries.size * (barHeight + barGap)
        val sorted = entries.sortedByDescending { it.bedTime }

        // Time labels (18:00 -> 18:00)
        val timeLabels = listOf(
            "18:00", "20:00", "22:00", "00:00", "02:00", "04:00",
            "06:00", "08:00", "10:00", "12:00", "14:00", "16:00", "18:00"
        )
        timeLabels.forEachIndexed { i, label ->
            val x = margin + (i / 12f) * contentWidth
            canvas.drawLine(x, timelineTop, x, timelineTop + timelineHeight, gridPaint)
            canvas.drawText(label, x - 12f, timelineTop - 4f, labelPaint)
        }

        // Median lines
        val bedNorms = sorted.map { DateTimeUtil.normalizeToAxis(it.bedTime) }
        val wakeNorms = sorted.map { DateTimeUtil.normalizeToAxis(it.wakeTime) }
        val medBed = bedNorms.sorted().let { it[it.size / 2] }
        val medWake = wakeNorms.sorted().let { it[it.size / 2] }
        val medBedX = margin + medBed * contentWidth
        val medWakeX = margin + medWake * contentWidth
        canvas.drawLine(medBedX, timelineTop, medBedX, timelineTop + timelineHeight, medianBedPaint)
        canvas.drawLine(
            medWakeX, timelineTop, medWakeX, timelineTop + timelineHeight, medianWakePaint
        )

        // Entry bars
        sorted.forEachIndexed { i, entry ->
            val barY = timelineTop + i * (barHeight + barGap)
            val bedNorm = DateTimeUtil.normalizeToAxis(entry.bedTime)
            val wakeNorm = DateTimeUtil.normalizeToAxis(entry.wakeTime)
            val startX = margin + bedNorm * contentWidth
            val endX = margin + wakeNorm * contentWidth
            val barW = (endX - startX).coerceAtLeast(4f)

            val totalMin = (entry.wakeTime - entry.bedTime) / 60_000f
            val latFraction = if (totalMin > 0) entry.sleepLatency / totalMin else 0f
            val latW = barW * latFraction

            // Latency
            canvas.drawRect(startX, barY, startX + latW, barY + barHeight, latencyPaint)

            // Sleep bar (quality colored)
            val sleepPaint = Paint().apply {
                color = qualityColorInt(entry.quality)
            }
            canvas.drawRect(startX + latW, barY, endX, barY + barHeight, sleepPaint)

            // Wake windows
            entry.wakeWindows.forEach { window ->
                val ws = DateTimeUtil.normalizeToAxis(window.start)
                val we = DateTimeUtil.normalizeToAxis(window.end)
                val wx = margin + ws * contentWidth
                val ww = ((we - ws) * contentWidth).coerceAtLeast(2f)
                canvas.drawRect(wx, barY, wx + ww, barY + barHeight, wakePaint)
            }

            // Date label on right
            val dateLabel = DateTimeUtil.formatDateShort(entry.date)
            canvas.drawText(dateLabel, endX + 6f, barY + barHeight - 2f, labelPaint)
        }

        document.finishPage(page)

        // Write file
        val fileName = "SchlafGut_Bericht_${startDate}_${endDate}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun qualityColorInt(quality: Int): Int = when {
        quality >= 8 -> Color.rgb(16, 185, 129)    // Emerald
        quality >= 5 -> Color.rgb(99, 102, 241)     // Indigo
        else -> Color.rgb(244, 63, 94)              // Rose
    }
}
