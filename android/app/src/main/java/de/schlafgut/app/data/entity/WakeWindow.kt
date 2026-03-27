package de.schlafgut.app.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class WakeWindow(
    val start: Long,       // Epoch millis
    val end: Long,         // Epoch millis
    val outOfBed: Boolean = false,
    val outOfBedStart: Long? = null,
    val outOfBedEnd: Long? = null
) {
    val durationMinutes: Int
        get() = ((end - start) / 60_000).toInt()
        
    val outOfBedDurationMinutes: Int
        get() = if (outOfBed && outOfBedStart != null && outOfBedEnd != null) {
            ((outOfBedEnd - outOfBedStart) / 60_000).toInt()
        } else 0
}
