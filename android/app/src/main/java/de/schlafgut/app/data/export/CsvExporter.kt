package de.schlafgut.app.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.util.DateTimeUtil
import java.io.File
import java.util.Locale

object CsvExporter {

    fun export(
        context: Context,
        entries: List<SleepEntryEntity>,
        healthSnapshots: Map<String, HealthSnapshotEntity> = emptyMap(),
        startDate: String = "",
        endDate: String = ""
    ): Uri? {
        if (entries.isEmpty()) return null

        val sb = StringBuilder()

        sb.appendLine(
            "Datum;Typ;Bettzeit;Aufwachzeit;Schlafdauer (h);Wachzeit (min);" +
                "Einschlafzeit (min);Qualität;Unterbrechungen;" +
                "Gewicht (kg);Ruhepuls;SpO2 (%);Schritte;Notizen"
        )

        entries.sortedBy { it.bedTime }.forEach { entry ->
            val snapshot = healthSnapshots[entry.id]
            val dateStr = DateTimeUtil.formatDateShort(entry.date)
            val typ = if (entry.isNap) "Nickerchen" else "Nachtschlaf"
            val bedStr = DateTimeUtil.formatTime(entry.bedTime)
            val wakeStr = DateTimeUtil.formatTime(entry.wakeTime)
            val durationH = String.format(Locale.US, "%.2f", entry.sleepDurationMinutes / 60.0)
            val weight = snapshot?.weightKg?.let { String.format(Locale.US, "%.1f", it) } ?: ""
            val hr = snapshot?.restingHeartRate?.toString() ?: ""
            val spo2 = snapshot?.oxygenSaturation?.let { String.format(Locale.US, "%.0f", it) } ?: ""
            val steps = snapshot?.stepsTotal?.toString() ?: ""
            val notes = "\"${entry.notes.replace("\"", "\"\"")}\""

            sb.appendLine(
                "$dateStr;$typ;$bedStr;$wakeStr;$durationH;${entry.wakeDurationMinutes};" +
                    "${entry.sleepLatency};${entry.quality};${entry.interruptionCount};" +
                    "$weight;$hr;$spo2;$steps;$notes"
            )
        }

        sb.appendLine()
        sb.appendLine("Zusammenfassung")
        sb.appendLine(
            "Ø Dauer;${
                String.format(Locale.US, "%.2f", entries.map { it.sleepDurationMinutes }.average() / 60.0)
            }h"
        )
        sb.appendLine(
            "Ø Qualität;${String.format(Locale.US, "%.1f", entries.map { it.quality }.average())}"
        )
        sb.appendLine("Einträge;${entries.size}")

        val fileName = "SchlafGut_Export_${startDate}_${endDate}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(sb.toString(), Charsets.UTF_8)

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
