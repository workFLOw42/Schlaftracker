package de.schlafgut.app.util

import de.schlafgut.app.data.entity.WakeWindow

object SleepCalculator {

    /**
     * Berechnet die tatsächliche Schlafdauer in Minuten.
     * Formel: (wakeTime - bedTime) - sleepLatency - summe(wakeWindows)
     *
     * Wachphasen werden defensiv auf die Schlafperiode begrenzt, damit Altdaten
     * (z. B. aus Backups) mit Fenstern außerhalb die Dauer nicht verfälschen.
     */
    fun calculateSleepDuration(
        bedTime: Long,
        wakeTime: Long,
        sleepLatencyMinutes: Int,
        wakeWindows: List<WakeWindow>
    ): Int {
        val totalTimeInBedMinutes = ((wakeTime - bedTime) / 60_000).toInt()
        val totalWakeMinutes = wakeWindows.sumOf { clampedMinutes(it, bedTime, wakeTime) }
        return totalTimeInBedMinutes - sleepLatencyMinutes - totalWakeMinutes
    }

    /** Anteil einer Wachphase (in Minuten), der tatsächlich in [bedTime, wakeTime] liegt. */
    private fun clampedMinutes(window: WakeWindow, bedTime: Long, wakeTime: Long): Int {
        val start = window.start.coerceAtLeast(bedTime)
        val end = window.end.coerceAtMost(wakeTime)
        return if (end > start) ((end - start) / 60_000).toInt() else 0
    }

    /**
     * Liefert den Index der ersten Wachphase, die außerhalb der Schlafzeit liegt
     * oder sich mit einer anderen überschneidet, sonst null.
     */
    fun findInvalidWakeWindow(wakeWindows: List<WakeWindow>, bedTime: Long, wakeTime: Long): Int? {
        wakeWindows.forEachIndexed { i, w ->
            if (!validateWakeWindow(w, bedTime, wakeTime)) return i
        }
        val sorted = wakeWindows.withIndex().sortedBy { it.value.start }
        for (k in 1 until sorted.size) {
            if (sorted[k].value.start < sorted[k - 1].value.end) return sorted[k].index
        }
        return null
    }

    /**
     * Bringt Wachphasen in einen gültigen Zustand für die Schlafperiode [bedTime, wakeTime]:
     * - Phasen werden auf die Schlafperiode begrenzt, komplett außerhalb liegende entfallen.
     * - Überschneidende Phasen entfallen (die frühere bleibt erhalten).
     * - Aufsteh-Phasen werden auf ihre Wachphase begrenzt.
     * Ergebnis ist nach Startzeit sortiert.
     */
    fun sanitizeWakeWindows(wakeWindows: List<WakeWindow>, bedTime: Long, wakeTime: Long): List<WakeWindow> {
        val result = mutableListOf<WakeWindow>()
        for (w in wakeWindows.sortedBy { it.start }) {
            val start = w.start.coerceAtLeast(bedTime)
            val end = w.end.coerceAtMost(wakeTime)
            if (end <= start) continue
            if (result.isNotEmpty() && start < result.last().end) continue
            val periods = w.outOfBedPeriods.mapNotNull { p ->
                val ps = p.start.coerceAtLeast(start)
                val pe = p.end.coerceAtMost(end)
                if (pe > ps) p.copy(start = ps, end = pe) else null
            }
            result += w.copy(start = start, end = end, outOfBedPeriods = periods)
        }
        return result
    }

    /** Prüft, ob sich eine Wachphase mit bestehenden überschneidet. */
    fun overlapsExisting(window: WakeWindow, existing: List<WakeWindow>): Boolean =
        existing.any { window.start < it.end && it.start < window.end }

    /**
     * Berechnet die gesamte Wachzeit innerhalb der Schlafperiode.
     */
    fun calculateWakeDuration(wakeWindows: List<WakeWindow>): Int =
        wakeWindows.sumOf { it.durationMinutes }

    /**
     * Validierung: Aufwachzeit muss nach Bettzeit liegen.
     */
    fun validateTimeOrder(bedTime: Long, wakeTime: Long): Boolean =
        wakeTime > bedTime

    /**
     * Validierung: Berechnete Schlafdauer muss >= 0 sein.
     */
    fun validatePositiveDuration(
        bedTime: Long,
        wakeTime: Long,
        sleepLatencyMinutes: Int,
        wakeWindows: List<WakeWindow>
    ): Boolean =
        calculateSleepDuration(bedTime, wakeTime, sleepLatencyMinutes, wakeWindows) >= 0

    /**
     * Validierung: Wachphase muss innerhalb der Schlafzeit liegen.
     */
    fun validateWakeWindow(
        window: WakeWindow,
        bedTime: Long,
        wakeTime: Long
    ): Boolean =
        window.start >= bedTime &&
            window.end <= wakeTime &&
            window.end > window.start
}
