package de.schlafgut.app.util

import de.schlafgut.app.data.entity.WakeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepCalculatorTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    // Fester Basis-Zeitpunkt (zeitzonenunabhängig, da nur Differenzen zählen)
    private val bed = 1_700_000_000_000L
    private val wake = bed + 8 * hour

    private fun window(startOffsetMin: Long, durationMin: Long) =
        WakeWindow(start = bed + startOffsetMin * minute, end = bed + (startOffsetMin + durationMin) * minute)

    // --- sanitizeWakeWindows ---

    @Test
    fun sanitize_dropsWindowsCompletelyOutside() {
        val result = SleepCalculator.sanitizeWakeWindows(
            listOf(window(-120, 30), window(60, 10), window(600, 20)), bed, wake
        )
        assertEquals(listOf(window(60, 10)), result)
    }

    @Test
    fun sanitize_clampsPartiallyOutsideWindows() {
        val result = SleepCalculator.sanitizeWakeWindows(
            listOf(window(-10, 20), window(470, 30)), bed, wake
        )
        assertEquals(listOf(window(0, 10), window(470, 10)), result)
    }

    @Test
    fun sanitize_dropsOverlappingWindows() {
        val result = SleepCalculator.sanitizeWakeWindows(
            listOf(window(100, 30), window(110, 10)), bed, wake
        )
        assertEquals(listOf(window(100, 30)), result)
    }

    @Test
    fun sanitize_resultNeverYieldsNegativeDuration() {
        val windows = SleepCalculator.sanitizeWakeWindows(
            listOf(window(-600, 2000)), bed, wake
        )
        assertTrue(SleepCalculator.calculateSleepDuration(bed, wake, 0, windows) >= 0)
    }

    // --- calculateSleepDuration ---

    @Test
    fun sleepDuration_noWakeWindows_noLatency_equalsTimeInBed() {
        assertEquals(480, SleepCalculator.calculateSleepDuration(bed, wake, 0, emptyList()))
    }

    @Test
    fun sleepDuration_subtractsLatency() {
        assertEquals(465, SleepCalculator.calculateSleepDuration(bed, wake, 15, emptyList()))
    }

    @Test
    fun sleepDuration_subtractsLatencyAndAllWakeWindows() {
        val windows = listOf(window(60, 10), window(240, 25))
        assertEquals(480 - 15 - 35, SleepCalculator.calculateSleepDuration(bed, wake, 15, windows))
    }

    @Test
    fun sleepDuration_truncatesPartialMinutes() {
        val wakeWithSeconds = bed + 90 * minute + 59_999L
        assertEquals(90, SleepCalculator.calculateSleepDuration(bed, wakeWithSeconds, 0, emptyList()))
    }

    @Test
    fun sleepDuration_latencyLongerThanTimeInBed_isNegative() {
        val shortWake = bed + 30 * minute
        assertEquals(-15, SleepCalculator.calculateSleepDuration(bed, shortWake, 45, emptyList()))
    }

    @Test
    fun sleepDuration_wakeBeforeBed_isNegative() {
        assertEquals(-60, SleepCalculator.calculateSleepDuration(bed, bed - hour, 0, emptyList()))
    }

    @Test
    fun sleepDuration_wakeWindowOutsideSleepPeriod_isIgnored() {
        val outside = WakeWindow(start = wake + hour, end = wake + hour + 20 * minute)
        assertEquals(480, SleepCalculator.calculateSleepDuration(bed, wake, 0, listOf(outside)))
    }

    @Test
    fun sleepDuration_wakeWindowPartiallyOutside_onlyInsidePartSubtracted() {
        val partial = WakeWindow(start = wake - 10 * minute, end = wake + 30 * minute)
        assertEquals(470, SleepCalculator.calculateSleepDuration(bed, wake, 0, listOf(partial)))
    }

    // --- findInvalidWakeWindow / overlapsExisting ---

    @Test
    fun findInvalidWakeWindow_allValid_isNull() {
        assertEquals(null, SleepCalculator.findInvalidWakeWindow(listOf(window(60, 10), window(120, 10)), bed, wake))
    }

    @Test
    fun findInvalidWakeWindow_outside_returnsIndex() {
        val outside = WakeWindow(start = wake + hour, end = wake + 2 * hour)
        assertEquals(1, SleepCalculator.findInvalidWakeWindow(listOf(window(60, 10), outside), bed, wake))
    }

    @Test
    fun findInvalidWakeWindow_overlapping_returnsIndex() {
        assertEquals(1, SleepCalculator.findInvalidWakeWindow(listOf(window(60, 30), window(80, 10)), bed, wake))
    }

    @Test
    fun overlapsExisting_detectsOverlapAndAdjacency() {
        val existing = listOf(window(60, 30))
        assertTrue(SleepCalculator.overlapsExisting(window(80, 20), existing))
        assertFalse(SleepCalculator.overlapsExisting(window(90, 10), existing))
    }

    // --- calculateWakeDuration ---

    @Test
    fun wakeDuration_empty_isZero() {
        assertEquals(0, SleepCalculator.calculateWakeDuration(emptyList()))
    }

    @Test
    fun wakeDuration_sumsAllWindows() {
        val windows = listOf(window(10, 5), window(100, 12), window(300, 3))
        assertEquals(20, SleepCalculator.calculateWakeDuration(windows))
    }

    // --- validateTimeOrder ---

    @Test
    fun validateTimeOrder_wakeAfterBed_isTrue() {
        assertTrue(SleepCalculator.validateTimeOrder(bed, wake))
    }

    @Test
    fun validateTimeOrder_equalTimes_isFalse() {
        assertFalse(SleepCalculator.validateTimeOrder(bed, bed))
    }

    @Test
    fun validateTimeOrder_wakeBeforeBed_isFalse() {
        assertFalse(SleepCalculator.validateTimeOrder(wake, bed))
    }

    // --- validatePositiveDuration ---

    @Test
    fun validatePositiveDuration_normalNight_isTrue() {
        assertTrue(SleepCalculator.validatePositiveDuration(bed, wake, 15, listOf(window(60, 30))))
    }

    @Test
    fun validatePositiveDuration_exactlyZero_isTrue() {
        val w = bed + 30 * minute
        assertTrue(SleepCalculator.validatePositiveDuration(bed, w, 30, emptyList()))
    }

    @Test
    fun validatePositiveDuration_latencyGreaterThanDuration_isFalse() {
        val w = bed + 30 * minute
        assertFalse(SleepCalculator.validatePositiveDuration(bed, w, 31, emptyList()))
    }

    @Test
    fun validatePositiveDuration_wakeWindowsExceedDuration_isFalse() {
        val w = bed + 60 * minute
        val windows = listOf(window(0, 40), window(40, 20))
        assertFalse(SleepCalculator.validatePositiveDuration(bed, w, 1, windows))
    }

    // --- validateWakeWindow ---

    @Test
    fun validateWakeWindow_inside_isTrue() {
        assertTrue(SleepCalculator.validateWakeWindow(window(60, 10), bed, wake))
    }

    @Test
    fun validateWakeWindow_exactlyAtBoundaries_isTrue() {
        assertTrue(SleepCalculator.validateWakeWindow(WakeWindow(bed, wake), bed, wake))
    }

    @Test
    fun validateWakeWindow_startsBeforeBed_isFalse() {
        assertFalse(SleepCalculator.validateWakeWindow(WakeWindow(bed - minute, bed + 10 * minute), bed, wake))
    }

    @Test
    fun validateWakeWindow_endsAfterWake_isFalse() {
        assertFalse(SleepCalculator.validateWakeWindow(WakeWindow(wake - 10 * minute, wake + minute), bed, wake))
    }

    @Test
    fun validateWakeWindow_completelyOutside_isFalse() {
        assertFalse(SleepCalculator.validateWakeWindow(WakeWindow(wake + hour, wake + 2 * hour), bed, wake))
    }

    @Test
    fun validateWakeWindow_zeroLength_isFalse() {
        val t = bed + hour
        assertFalse(SleepCalculator.validateWakeWindow(WakeWindow(t, t), bed, wake))
    }

    @Test
    fun validateWakeWindow_endBeforeStart_isFalse() {
        assertFalse(SleepCalculator.validateWakeWindow(WakeWindow(bed + 2 * hour, bed + hour), bed, wake))
    }
}
