package de.schlafgut.app.data.entity

import kotlinx.serialization.Serializable

/**
 * Einzelne Phase, in der der Nutzer während einer Wachphase aufgestanden ist.
 */
@Serializable
data class OutOfBedPeriod(
    val start: Long,   // Epoch millis
    val end: Long       // Epoch millis
) {
    val durationMinutes: Int
        get() = ((end - start) / 60_000).toInt()
}

/**
 * Eine Wachphase innerhalb einer Schlafperiode.
 * Kann mehrere Aufsteh-Phasen enthalten.
 */
@Serializable
data class WakeWindow(
    val start: Long,                               // Epoch millis
    val end: Long,                                 // Epoch millis
    val outOfBedPeriods: List<OutOfBedPeriod> = emptyList(),

    // Legacy-Felder für Migration – werden nicht mehr aktiv genutzt
    @Deprecated("Use outOfBedPeriods instead")
    val outOfBed: Boolean = false,
    @Deprecated("Use outOfBedPeriods instead")
    val outOfBedStart: Long? = null,
    @Deprecated("Use outOfBedPeriods instead")
    val outOfBedEnd: Long? = null
) {
    val durationMinutes: Int
        get() = ((end - start) / 60_000).toInt()

    val totalOutOfBedMinutes: Int
        get() = outOfBedPeriods.sumOf { it.durationMinutes }

    val hasOutOfBed: Boolean
        get() = outOfBedPeriods.isNotEmpty()
}
