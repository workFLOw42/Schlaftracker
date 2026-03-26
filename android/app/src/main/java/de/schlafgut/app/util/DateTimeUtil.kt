package de.schlafgut.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

object DateTimeUtil {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val locale: Locale = Locale.GERMAN

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("d. MMM", locale)

    fun epochToLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    fun localDateTimeToEpoch(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    fun localDateToEpoch(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun epochToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * Formatiert Epoch millis als "HH:mm" (z.B. "23:15")
     */
    fun formatTime(epochMillis: Long): String =
        epochToLocalDateTime(epochMillis).format(timeFormatter)

    /**
     * Formatiert Epoch millis als "Montag, 1. Apr." (deutsches Format)
     */
    fun formatDateFull(epochMillis: Long): String {
        val dt = epochToLocalDateTime(epochMillis)
        val dayOfWeek = dt.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val datePart = dt.format(dateFormatter)
        return "$dayOfWeek, $datePart"
    }

    /**
     * Formatiert Epoch millis als "1. Apr." (kurzes deutsches Format)
     */
    fun formatDateShort(epochMillis: Long): String =
        epochToLocalDateTime(epochMillis).format(dateFormatter)

    /**
     * Formatiert Minuten als "7h 30m" oder "45m"
     */
    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /**
     * Formatiert Minuten als Dezimalstunden (z.B. 7.5)
     */
    fun minutesToHours(minutes: Int): Double =
        minutes / 60.0

    /**
     * Default-Bettzeit: Gestern 23:00
     */
    fun defaultBedTime(): Long {
        val yesterday = LocalDate.now().minusDays(1)
        return localDateTimeToEpoch(LocalDateTime.of(yesterday, LocalTime.of(23, 0)))
    }

    /**
     * Default-Aufwachzeit: Heute 7:00
     */
    fun defaultWakeTime(): Long {
        val today = LocalDate.now()
        return localDateTimeToEpoch(LocalDateTime.of(today, LocalTime.of(7, 0)))
    }

    /**
     * Default-Bettzeit für Nickerchen: Heute 13:00
     */
    fun defaultNapBedTime(): Long {
        val today = LocalDate.now()
        return localDateTimeToEpoch(LocalDateTime.of(today, LocalTime.of(13, 0)))
    }

    /**
     * Default-Aufwachzeit für Nickerchen: Heute 13:45
     */
    fun defaultNapWakeTime(): Long {
        val today = LocalDate.now()
        return localDateTimeToEpoch(LocalDateTime.of(today, LocalTime.of(13, 45)))
    }

    /**
     * Startdatum für Statistik-Filter: Vor n Tagen
     */
    fun daysAgo(days: Int): Long =
        localDateToEpoch(LocalDate.now().minusDays(days.toLong()))

    fun today(): Long =
        localDateToEpoch(LocalDate.now())

    fun todayEnd(): Long =
        localDateTimeToEpoch(LocalDateTime.of(LocalDate.now(), LocalTime.MAX))
}
