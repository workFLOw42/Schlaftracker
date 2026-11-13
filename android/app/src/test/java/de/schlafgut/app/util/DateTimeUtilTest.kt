package de.schlafgut.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone

/**
 * DateTimeUtil liest die Zeitzone bei jedem Aufruf neu (kein Cache). Die meisten Tests erzeugen
 * Zeitstempel trotzdem über DateTimeUtil selbst (lokale Uhrzeit -> Epoch) und prüfen die
 * Formatierung zurück – das ist unabhängig von der jeweiligen Default-Zeitzone.
 * Gewählte Zeitpunkte liegen außerhalb von Sommerzeit-Umstellungen.
 */
class DateTimeUtilTest {

    @Test
    fun formatTime_followsTimeZoneChangeAtRuntime() {
        val original = TimeZone.getDefault()
        try {
            val instant = Instant.parse("2025-01-15T12:00:00Z").toEpochMilli()
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            assertEquals("13:00", DateTimeUtil.formatTime(instant))
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals("07:00", DateTimeUtil.formatTime(instant))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    private fun epochOf(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        DateTimeUtil.localDateTimeToEpoch(LocalDateTime.of(y, mo, d, h, mi))

    // --- formatDuration ---

    @Test
    fun formatDuration_zero() {
        assertEquals("0m", DateTimeUtil.formatDuration(0))
    }

    @Test
    fun formatDuration_onlyMinutes() {
        assertEquals("45m", DateTimeUtil.formatDuration(45))
        assertEquals("59m", DateTimeUtil.formatDuration(59))
    }

    @Test
    fun formatDuration_exactHour() {
        assertEquals("1h 0m", DateTimeUtil.formatDuration(60))
    }

    @Test
    fun formatDuration_hoursAndMinutes() {
        assertEquals("7h 30m", DateTimeUtil.formatDuration(450))
        assertEquals("25h 1m", DateTimeUtil.formatDuration(1501))
    }

    // --- minutesToHours ---

    @Test
    fun minutesToHours_converts() {
        assertEquals(7.5, DateTimeUtil.minutesToHours(450), 1e-9)
        assertEquals(0.0, DateTimeUtil.minutesToHours(0), 1e-9)
        assertEquals(0.25, DateTimeUtil.minutesToHours(15), 1e-9)
        assertEquals(-1.0, DateTimeUtil.minutesToHours(-60), 1e-9)
    }

    // --- Roundtrips ---

    @Test
    fun localDateTime_roundtrip() {
        val dt = LocalDateTime.of(2025, 1, 15, 23, 15)
        assertEquals(dt, DateTimeUtil.epochToLocalDateTime(DateTimeUtil.localDateTimeToEpoch(dt)))
    }

    @Test
    fun localDate_roundtrip() {
        val date = LocalDate.of(2025, 5, 12)
        assertEquals(date, DateTimeUtil.epochToLocalDate(DateTimeUtil.localDateToEpoch(date)))
    }

    @Test
    fun localDateToEpoch_isStartOfDay() {
        val date = LocalDate.of(2025, 5, 12)
        val dt = DateTimeUtil.epochToLocalDateTime(DateTimeUtil.localDateToEpoch(date))
        assertEquals(LocalDateTime.of(2025, 5, 12, 0, 0), dt)
    }

    // --- formatTime ---

    @Test
    fun formatTime_twoDigitPadding() {
        assertEquals("23:15", DateTimeUtil.formatTime(epochOf(2025, 1, 15, 23, 15)))
        assertEquals("07:05", DateTimeUtil.formatTime(epochOf(2025, 1, 15, 7, 5)))
        assertEquals("00:00", DateTimeUtil.formatTime(epochOf(2025, 1, 15, 0, 0)))
    }

    // --- formatDateShort / formatDateFull ---

    @Test
    fun formatDateShort_germanMonth() {
        // Mai hat im Deutschen keine Abkürzung -> unabhängig von CLDR-Punkt-Konventionen
        assertEquals("12. Mai", DateTimeUtil.formatDateShort(epochOf(2025, 5, 12, 22, 0)))
    }

    @Test
    fun formatDateFull_germanWeekday() {
        // 12.05.2025 ist ein Montag
        assertEquals("Montag, 12. Mai", DateTimeUtil.formatDateFull(epochOf(2025, 5, 12, 22, 0)))
    }

    @Test
    fun formatDateFull_singleDigitDay() {
        // 01.05.2025 ist ein Donnerstag
        assertEquals("Donnerstag, 1. Mai", DateTimeUtil.formatDateFull(epochOf(2025, 5, 1, 8, 0)))
    }

    // --- normalizeToAxis (18:00 -> 0.0) ---

    @Test
    fun normalizeToAxis_at18_isZero() {
        assertEquals(0f, DateTimeUtil.normalizeToAxis(epochOf(2025, 1, 15, 18, 0)), 1e-6f)
    }

    @Test
    fun normalizeToAxis_atMidnight_isQuarter() {
        assertEquals(0.25f, DateTimeUtil.normalizeToAxis(epochOf(2025, 1, 16, 0, 0)), 1e-6f)
    }

    @Test
    fun normalizeToAxis_at6_isHalf() {
        assertEquals(0.5f, DateTimeUtil.normalizeToAxis(epochOf(2025, 1, 16, 6, 0)), 1e-6f)
    }

    @Test
    fun normalizeToAxis_atNoon_isThreeQuarters() {
        assertEquals(0.75f, DateTimeUtil.normalizeToAxis(epochOf(2025, 1, 16, 12, 0)), 1e-6f)
    }

    @Test
    fun normalizeToAxis_justBefore18_isAlmostOne() {
        assertEquals(1439f / 1440f, DateTimeUtil.normalizeToAxis(epochOf(2025, 1, 16, 17, 59)), 1e-6f)
    }
}
