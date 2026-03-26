package de.schlafgut.app.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class WakeWindow(
    val start: Long,  // Epoch millis
    val end: Long     // Epoch millis
) {
    val durationMinutes: Int
        get() = ((end - start) / 60_000).toInt()
}
