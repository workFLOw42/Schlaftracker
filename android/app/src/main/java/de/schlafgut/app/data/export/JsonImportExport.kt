package de.schlafgut.app.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.entity.WakeWindow
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
    val notes: String = ""
)

@Serializable
data class ExportWakeWindow(
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
                wakeWindows = entry.wakeWindows.map {
                    ExportWakeWindow(epochToIso(it.start), epochToIso(it.end))
                },
                interruptionCount = entry.interruptionCount,
                quality = entry.quality,
                tags = entry.tags,
                notes = entry.notes
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
        val entries = data.entries.map { e ->
            SleepEntryEntity(
                id = e.id,
                isNap = e.isNap,
                date = isoToEpoch(e.date),
                bedTime = isoToEpoch(e.bedTime),
                wakeTime = isoToEpoch(e.wakeTime),
                sleepLatency = e.sleepLatency,
                sleepDurationMinutes = e.sleepDurationMinutes,
                wakeDurationMinutes = e.wakeDurationMinutes,
                wakeWindows = e.wakeWindows.map {
                    WakeWindow(isoToEpoch(it.start), isoToEpoch(it.end))
                },
                interruptionCount = e.interruptionCount,
                quality = e.quality,
                tags = e.tags,
                notes = e.notes
            )
        }
        val settings = data.settings?.let {
            UserSettingsEntity(
                userName = it.userName,
                defaultSleepLatency = it.defaultSleepLatency
            )
        }
        return ImportResult(entries, settings)
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
                SleepEntryEntity(
                    id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                    isNap = obj["isNap"]?.jsonPrimitive?.boolean ?: false,
                    date = isoToEpoch(obj["date"]?.jsonPrimitive?.content ?: return@mapNotNull null),
                    bedTime = isoToEpoch(
                        obj["bedTime"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    ),
                    wakeTime = isoToEpoch(
                        obj["wakeTime"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    ),
                    sleepLatency = obj["sleepLatency"]?.jsonPrimitive?.int ?: 15,
                    sleepDurationMinutes = obj["sleepDurationMinutes"]?.jsonPrimitive?.int ?: 0,
                    wakeDurationMinutes = obj["wakeDurationMinutes"]?.jsonPrimitive?.int ?: 0,
                    wakeWindows = obj["wakeWindows"]?.jsonArray?.map { w ->
                        val wObj = w.jsonObject
                        WakeWindow(
                            isoToEpoch(wObj["start"]!!.jsonPrimitive.content),
                            isoToEpoch(wObj["end"]!!.jsonPrimitive.content)
                        )
                    } ?: emptyList(),
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

    private fun isoToEpoch(isoString: String): Long =
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
                0L
            }
        }
}
