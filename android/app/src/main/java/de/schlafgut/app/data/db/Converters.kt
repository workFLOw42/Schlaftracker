package de.schlafgut.app.data.db

import androidx.room.TypeConverter
import de.schlafgut.app.data.entity.MedicationEntry
import de.schlafgut.app.data.entity.OutOfBedPeriod
import de.schlafgut.app.data.entity.WakeWindow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromWakeWindowList(value: List<WakeWindow>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toWakeWindowList(value: String): List<WakeWindow> {
        val windows: List<WakeWindow> = json.decodeFromString(value)
        // Legacy-Migration: Einzelne outOfBed-Felder → outOfBedPeriods
        return windows.map { w ->
            @Suppress("DEPRECATION")
            if (w.outOfBedPeriods.isEmpty() && w.outOfBed && w.outOfBedStart != null && w.outOfBedEnd != null) {
                w.copy(
                    outOfBedPeriods = listOf(
                        OutOfBedPeriod(start = w.outOfBedStart, end = w.outOfBedEnd)
                    )
                )
            } else w
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        json.decodeFromString(value)

    @TypeConverter
    fun fromMedicationList(value: List<MedicationEntry>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toMedicationList(value: String): List<MedicationEntry> =
        json.decodeFromString(value)
}
