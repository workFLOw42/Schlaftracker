package de.schlafgut.app.util

import de.schlafgut.app.data.entity.WakeWindow

object SleepCalculator {

    /**
     * Berechnet die tatsächliche Schlafdauer in Minuten.
     * Formel: (wakeTime - bedTime) - sleepLatency - summe(wakeWindows)
     */
    fun calculateSleepDuration(
        bedTime: Long,
        wakeTime: Long,
        sleepLatencyMinutes: Int,
        wakeWindows: List<WakeWindow>
    ): Int {
        val totalTimeInBedMinutes = ((wakeTime - bedTime) / 60_000).toInt()
        val totalWakeMinutes = wakeWindows.sumOf { it.durationMinutes }
        return totalTimeInBedMinutes - sleepLatencyMinutes - totalWakeMinutes
    }

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
