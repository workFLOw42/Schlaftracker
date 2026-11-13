package de.schlafgut.app.data.export

import de.schlafgut.app.data.entity.OutOfBedPeriod
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.entity.WakeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JsonImportExportTest {

    private val bed = Instant.parse("2025-05-12T21:30:00Z").toEpochMilli()
    private val wake = Instant.parse("2025-05-13T05:45:00Z").toEpochMilli()
    private val date = Instant.parse("2025-05-12T00:00:00Z").toEpochMilli()

    private fun entry(
        id: String = "e1",
        alcoholLevel: Int = 0,
        drugLevel: Int = 0,
        sleepAid: Boolean = false,
        medication: Boolean = false,
        medicationNotes: String = "",
        wakeWindows: List<WakeWindow> = listOf(
            WakeWindow(
                Instant.parse("2025-05-13T01:00:00Z").toEpochMilli(),
                Instant.parse("2025-05-13T01:20:00Z").toEpochMilli()
            ),
            WakeWindow(
                Instant.parse("2025-05-13T03:10:00Z").toEpochMilli(),
                Instant.parse("2025-05-13T03:15:00Z").toEpochMilli()
            )
        )
    ) = SleepEntryEntity(
        id = id,
        isNap = false,
        date = date,
        bedTime = bed,
        wakeTime = wake,
        sleepLatency = 15,
        sleepDurationMinutes = 470,
        wakeDurationMinutes = 25,
        wakeWindows = wakeWindows,
        interruptionCount = wakeWindows.size,
        quality = 7,
        tags = listOf("Stress", "Sport"),
        notes = "Notiz mit Umlauten äöü",
        alcoholLevel = alcoholLevel,
        drugLevel = drugLevel,
        sleepAid = sleepAid,
        medication = medication,
        medicationNotes = medicationNotes
    )

    @Test
    fun exportThenImport_roundtrip_preservesCoreFieldsAndWakeWindows() {
        val entries = listOf(
            entry(id = "e1"),
            entry(id = "e2", wakeWindows = emptyList()).copy(isNap = true, quality = 3, tags = emptyList(), notes = "")
        )
        val settings = UserSettingsEntity(userName = "Flo", defaultSleepLatency = 20)

        val jsonString = JsonImportExport.exportToJson(entries, settings)
        val result = JsonImportExport.importFromJson(jsonString)

        // Substanzfelder sind hier auf Default -> vollständige Gleichheit erwartbar
        assertEquals(entries, result.entries)
        assertEquals(0, result.skippedCount)
        assertNotNull(result.settings)
        assertEquals("Flo", result.settings!!.userName)
        assertEquals(20, result.settings!!.defaultSleepLatency)
    }

    @Test
    fun export_containsExpectedStructure() {
        val jsonString = JsonImportExport.exportToJson(listOf(entry()), null)
        assertTrue(jsonString.contains("\"version\": 1"))
        assertTrue(jsonString.contains("\"source\": \"schlafgut-android\""))
        assertTrue(jsonString.contains("\"bedTime\": \"2025-05-12T21:30:00Z\""))
        assertTrue(jsonString.contains("\"start\": \"2025-05-13T01:00:00Z\""))
    }

    @Test
    fun exportThenImport_withoutSettings_hasNullSettings() {
        val result = JsonImportExport.importFromJson(JsonImportExport.exportToJson(listOf(entry()), null))
        assertNull(result.settings)
        assertEquals(1, result.entries.size)
    }

    @Test
    fun exportThenImport_emptyList() {
        val result = JsonImportExport.importFromJson(JsonImportExport.exportToJson(emptyList(), null))
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun exportThenImport_substanceFields_arePreserved() {
        val original = entry(
            alcoholLevel = 2, drugLevel = 1, sleepAid = true, medication = true,
            medicationNotes = "Ibuprofen"
        )
        val imported = JsonImportExport.importFromJson(
            JsonImportExport.exportToJson(listOf(original), null)
        ).entries.single()
        assertEquals(original, imported)
    }

    @Test
    fun exportThenImport_outOfBedPeriods_arePreserved() {
        val start = Instant.parse("2025-05-13T01:00:00Z").toEpochMilli()
        val end = Instant.parse("2025-05-13T01:30:00Z").toEpochMilli()
        val ww = WakeWindow(start, end, outOfBedPeriods = listOf(OutOfBedPeriod(start + 60_000, start + 600_000)))
        val imported = JsonImportExport.importFromJson(
            JsonImportExport.exportToJson(listOf(entry(wakeWindows = listOf(ww))), null)
        ).entries.single()
        assertEquals(ww, imported.wakeWindows.single())
    }

    @Test
    fun import_handwrittenExportFormat() {
        val jsonString = """
            {
              "version": 1,
              "exportDate": "2025-05-13T08:00:00Z",
              "source": "schlafgut-android",
              "unknownField": "ignored",
              "entries": [
                {
                  "id": "x1",
                  "date": "2025-05-12T00:00:00Z",
                  "bedTime": "2025-05-12T21:30:00Z",
                  "wakeTime": "2025-05-13T05:45:00Z",
                  "sleepLatency": 10,
                  "sleepDurationMinutes": 480,
                  "wakeDurationMinutes": 5,
                  "wakeWindows": [ { "start": "2025-05-13T02:00:00Z", "end": "2025-05-13T02:05:00Z" } ],
                  "interruptionCount": 1,
                  "quality": 8
                }
              ]
            }
        """.trimIndent()

        val result = JsonImportExport.importFromJson(jsonString)
        val e = result.entries.single()
        assertEquals("x1", e.id)
        assertEquals(false, e.isNap)
        assertEquals(bed, e.bedTime)
        assertEquals(wake, e.wakeTime)
        assertEquals(10, e.sleepLatency)
        assertEquals(1, e.wakeWindows.size)
        assertEquals(5, e.wakeWindows.single().durationMinutes)
        assertEquals(emptyList<String>(), e.tags)
        assertEquals("", e.notes)
        assertNull(result.settings)
    }

    @Test
    fun import_pwaLocalStorageFormat_skipsMalformedEntries() {
        val jsonString = """
            {
              "schlafgut_entries": [
                { "id": "p1", "date": "2025-05-12", "bedTime": "2025-05-12T21:30:00Z",
                  "wakeTime": "2025-05-13T05:45:00Z", "quality": 6, "tags": ["a"] },
                { "id": "broken", "date": "2025-05-12" }
              ],
              "schlafgut_users": [ { "name": "Anna", "settings": { "defaultSleepLatency": 25 } } ]
            }
        """.trimIndent()

        val result = JsonImportExport.importFromJson(jsonString)
        assertEquals(1, result.entries.size)
        assertEquals(1, result.skippedCount)
        val e = result.entries.single()
        assertEquals("p1", e.id)
        assertEquals(15, e.sleepLatency) // Default
        assertEquals(6, e.quality)
        assertEquals(listOf("a"), e.tags)
        assertEquals("Anna", result.settings!!.userName)
        assertEquals(25, result.settings!!.defaultSleepLatency)
    }

    @Test
    fun import_invalidTimes_areSkippedInsteadOf1970() {
        val jsonString = """
            {
              "version": 1,
              "entries": [
                { "id": "ok", "date": "2025-05-12T00:00:00Z", "bedTime": "2025-05-12T21:30:00Z",
                  "wakeTime": "2025-05-13T05:45:00Z", "sleepLatency": 10, "sleepDurationMinutes": 480,
                  "wakeDurationMinutes": 0, "interruptionCount": 0, "quality": 8 },
                { "id": "badBed", "date": "2025-05-12T00:00:00Z", "bedTime": "kaputt",
                  "wakeTime": "2025-05-13T05:45:00Z", "sleepLatency": 10, "sleepDurationMinutes": 480,
                  "wakeDurationMinutes": 0, "interruptionCount": 0, "quality": 8 },
                { "id": "reversed", "date": "2025-05-12T00:00:00Z", "bedTime": "2025-05-13T05:45:00Z",
                  "wakeTime": "2025-05-12T21:30:00Z", "sleepLatency": 10, "sleepDurationMinutes": 480,
                  "wakeDurationMinutes": 0, "interruptionCount": 0, "quality": 8 }
              ]
            }
        """.trimIndent()

        val result = JsonImportExport.importFromJson(jsonString)
        assertEquals(listOf("ok"), result.entries.map { it.id })
        assertEquals(2, result.skippedCount)
        assertTrue(result.entries.none { it.bedTime == 0L || it.wakeTime == 0L })
    }

    @Test
    fun import_wakeWindowsOutsideSleep_areRemoved() {
        val jsonString = """
            {
              "version": 1,
              "entries": [
                { "id": "w", "date": "2025-05-12T00:00:00Z", "bedTime": "2025-05-12T21:30:00Z",
                  "wakeTime": "2025-05-13T05:45:00Z", "sleepLatency": 10, "sleepDurationMinutes": 480,
                  "wakeDurationMinutes": 0, "interruptionCount": 2, "quality": 8,
                  "wakeWindows": [
                    { "start": "2025-05-13T02:00:00Z", "end": "2025-05-13T02:05:00Z" },
                    { "start": "2025-05-13T09:00:00Z", "end": "2025-05-13T10:00:00Z" },
                    { "start": "ungültig", "end": "2025-05-13T03:00:00Z" }
                  ] }
              ]
            }
        """.trimIndent()

        val e = JsonImportExport.importFromJson(jsonString).entries.single()
        assertEquals(1, e.wakeWindows.size)
        assertEquals(5, e.wakeWindows.single().durationMinutes)
    }

    @Test
    fun import_unknownFormat_returnsEmpty() {
        val result = JsonImportExport.importFromJson("""{ "foo": 1 }""")
        assertTrue(result.entries.isEmpty())
        assertNull(result.settings)
    }
}
