package de.schlafgut.app.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import de.schlafgut.app.data.entity.OutOfBedPeriod
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.entity.WakeWindow
import de.schlafgut.app.util.SleepCalculator
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// --- Export Format ---

@Serializable
data class ExportData(
    val version: Int = 1,
    val exportDate: String = Instant.now().toString(),
    val source: String = "schlafgut-android",
    val entries: List<ExportEntry>,
    val settings: ExportSettings? = null
)

@Serializable
data class ExportEntry(
    val id: String,
    val isNap: Boolean = false,
    val date: String,
    val bedTime: String,
    val wakeTime: String,
    val sleepLatency: Int,
    val sleepDurationMinutes: Int,
    val wakeDurationMinutes: Int,
    val wakeWindows: List<ExportWakeWindow> = emptyList(),
    val interruptionCount: Int,
    val quality: Int,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    // Substanzkonsum – optional, damit ältere Backups weiterhin importierbar sind
    val alcoholLevel: Int = 0,
    val drugLevel: Int = 0,
    val sleepAid: Boolean = false,
    val medication: Boolean = false,
    val medicationNotes: String = ""
)

@Serializable
data class ExportWakeWindow(
    val start: String,
    val end: String,
    val outOfBedPeriods: List<ExportOutOfBedPeriod> = emptyList()
)

@Serializable
data class ExportOutOfBedPeriod(
    val start: String,
    val end: String
)

@Serializable
data class ExportSettings(
    val userName: String = "",
    val defaultSleepLatency: Int = 15
)

object JsonImportExport {

    // --- Export ---

    fun exportToJson(
        entries: List<SleepEntryEntity>,
        settings: UserSettingsEntity?
    ): String {
        val exportEntries = entries.map { entry ->
            ExportEntry(
                id = entry.id,
                isNap = entry.isNap,
                date = epochToIso(entry.date),
                bedTime = epochToIso(entry.bedTime),
                wakeTime = epochToIso(entry.wakeTime),
                sleepLatency = entry.sleepLatency,
                sleepDurationMinutes = entry.sleepDurationMinutes,
                wakeDurationMinutes = entry.wakeDurationMinutes,
                wakeWindows = entry.wakeWindows.map { w ->
                    ExportWakeWindow(
                        start = epochToIso(w.start),
                        end = epochToIso(w.end),
                        outOfBedPeriods = w.outOfBedPeriods.map {
                            ExportOutOfBedPeriod(epochToIso(it.start), epochToIso(it.end))
                        }
                    )
                },
                interruptionCount = entry.interruptionCount,
                quality = entry.quality,
                tags = entry.tags,
                notes = entry.notes,
                alcoholLevel = entry.alcoholLevel,
                drugLevel = entry.drugLevel,
                sleepAid = entry.sleepAid,
                medication = entry.medication,
                medicationNotes = entry.medicationNotes
            )
        }
        val exportData = ExportData(
            entries = exportEntries,
            settings = settings?.let {
                ExportSettings(it.userName, it.defaultSleepLatency)
            }
        )
        return json.encodeToString(exportData)
    }

    fun exportToFile(context: Context, jsonString: String): Uri {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val file = File(context.cacheDir, "SchlafGut_Backup_$timestamp.json")
        file.writeText(jsonString, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- Import ---

    data class ImportResult(
        val entries: List<SleepEntryEntity>,
        val settings: UserSettingsEntity?,
        val skippedCount: Int = 0
    )

    fun importFromJson(jsonString: String): ImportResult {
        val root = Json.parseToJsonElement(jsonString).jsonObject

        // Detect format: SchlafGut Android/PWA export vs raw PWA localStorage
        return if (root.containsKey("version") && root.containsKey("entries")) {
            importSchlafGutFormat(root)
        } else if (root.containsKey("schlafgut_entries")) {
            importPwaLocalStorageFormat(root)
        } else {
            // Try as array of entries
            ImportResult(emptyList(), null)
        }
    }

    private fun importSchlafGutFormat(root: JsonObject): ImportResult {
        val data = json.decodeFromString<ExportData>(root.toString())
        val entries = data.entries.mapNotNull { e ->
            // Ungültige Pflicht-Zeitangaben -> Eintrag überspringen (statt 1.1.1970 zu speichern)
            val bedTime = isoToEpoch(e.bedTime) ?: return@mapNotNull null
            val wakeTime = isoToEpoch(e.wakeTime) ?: return@mapNotNull null
            if (wakeTime <= bedTime) return@mapNotNull null
            val date = isoToEpoch(e.date) ?: bedTime
            val wakeWindows = sanitizedWakeWindows(
                e.wakeWindows.mapNotNull { w ->
                    WakeWindow(
                        start = isoToEpoch(w.start) ?: return@mapNotNull null,
                        end = isoToEpoch(w.end) ?: return@mapNotNull null,
                        outOfBedPeriods = w.outOfBedPeriods.mapNotNull {
                            OutOfBedPeriod(
                                isoToEpoch(it.start) ?: return@mapNotNull null,
                                isoToEpoch(it.end) ?: return@mapNotNull null
                            )
                        }
                    )
                },
                bedTime, wakeTime
            )
            // Wurden Wachphasen gekürzt/entfernt, abgeleitete Werte neu berechnen
            val windowsChanged = wakeWindows.size != e.wakeWindows.size ||
                wakeWindows.zip(e.wakeWindows).any { (w, ew) -> epochToIso(w.start) != ew.start || epochToIso(w.end) != ew.end }
            SleepEntryEntity(
                id = e.id,
                isNap = e.isNap,
                date = date,
                bedTime = bedTime,
                wakeTime = wakeTime,
                sleepLatency = e.sleepLatency,
                sleepDurationMinutes = if (windowsChanged) {
                    SleepCalculator.calculateSleepDuration(bedTime, wakeTime, e.sleepLatency, wakeWindows).coerceAtLeast(0)
                } else e.sleepDurationMinutes,
                wakeDurationMinutes = if (windowsChanged) SleepCalculator.calculateWakeDuration(wakeWindows) else e.wakeDurationMinutes,
                wakeWindows = wakeWindows,
                interruptionCount = if (windowsChanged) wakeWindows.size else e.interruptionCount,
                quality = e.quality,
                tags = e.tags,
                notes = e.notes,
                alcoholLevel = e.alcoholLevel,
                drugLevel = e.drugLevel,
                sleepAid = e.sleepAid,
                medication = e.medication,
                medicationNotes = e.medicationNotes
            )
        }
        val settings = data.settings?.let {
            UserSettingsEntity(
                userName = it.userName,
                defaultSleepLatency = it.defaultSleepLatency
            )
        }
        return ImportResult(entries, settings, skippedCount = data.entries.size - entries.size)
    }

    /**
     * Import from PWA localStorage dump.
     * Expected format: { "schlafgut_entries": [...], "schlafgut_users": [...] }
     */
    private fun importPwaLocalStorageFormat(root: JsonObject): ImportResult {
        val entriesArray = root["schlafgut_entries"]?.jsonArray ?: JsonArray(emptyList())

        val entries = entriesArray.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val bedTime = obj["bedTime"]?.jsonPrimitive?.content?.let(::isoToEpoch)
                    ?: return@mapNotNull null
                val wakeTime = obj["wakeTime"]?.jsonPrimitive?.content?.let(::isoToEpoch)
                    ?: return@mapNotNull null
                if (wakeTime <= bedTime) return@mapNotNull null
                val date = obj["date"]?.jsonPrimitive?.content?.let(::isoToEpoch)
                    ?: return@mapNotNull null
                SleepEntryEntity(
                    id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                    isNap = obj["isNap"]?.jsonPrimitive?.boolean ?: false,
                    date = date,
                    bedTime = bedTime,
                    wakeTime = wakeTime,
                    sleepLatency = obj["sleepLatency"]?.jsonPrimitive?.int ?: 15,
                    sleepDurationMinutes = obj["sleepDurationMinutes"]?.jsonPrimitive?.int ?: 0,
                    wakeDurationMinutes = obj["wakeDurationMinutes"]?.jsonPrimitive?.int ?: 0,
                    wakeWindows = sanitizedWakeWindows(
                        obj["wakeWindows"]?.jsonArray?.mapNotNull { w ->
                            val wObj = w.jsonObject
                            WakeWindow(
                                wObj["start"]?.jsonPrimitive?.content?.let(::isoToEpoch) ?: return@mapNotNull null,
                                wObj["end"]?.jsonPrimitive?.content?.let(::isoToEpoch) ?: return@mapNotNull null
                            )
                        } ?: emptyList(),
                        bedTime, wakeTime
                    ),
                    interruptionCount = obj["interruptionCount"]?.jsonPrimitive?.int ?: 0,
                    quality = obj["quality"]?.jsonPrimitive?.int ?: 5,
                    tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    notes = obj["notes"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null // Skip malformed entries
            }
        }

        // Extract user settings from first user
        val usersArray = root["schlafgut_users"]?.jsonArray
        val settings = usersArray?.firstOrNull()?.jsonObject?.let { user ->
            val settingsObj = user["settings"]?.jsonObject
            UserSettingsEntity(
                userName = user["name"]?.jsonPrimitive?.content ?: "",
                defaultSleepLatency = settingsObj?.get("defaultSleepLatency")?.jsonPrimitive?.int
                    ?: 15
            )
        }

        return ImportResult(entries, settings, skippedCount = entriesArray.size - entries.size)
    }

    // --- Helpers ---

    private fun epochToIso(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).toString()

    /** Wachphasen außerhalb der Schlafzeit werden gekürzt bzw. verworfen. */
    private fun sanitizedWakeWindows(windows: List<WakeWindow>, bedTime: Long, wakeTime: Long) =
        SleepCalculator.sanitizeWakeWindows(windows, bedTime, wakeTime)

    /** Liefert null bei ungültigen Zeitangaben, damit der Eintrag übersprungen werden kann. */
    private fun isoToEpoch(isoString: String): Long? =
        try {
            Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            // Try parsing as date-only (e.g. "2026-03-25")
            try {
                java.time.LocalDate.parse(isoString)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
}
