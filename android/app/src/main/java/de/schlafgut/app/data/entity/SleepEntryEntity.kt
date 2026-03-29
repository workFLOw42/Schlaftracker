package de.schlafgut.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_entries")
data class SleepEntryEntity(
    @PrimaryKey val id: String,
    val isNap: Boolean = false,
    val date: Long,                      // Epoch millis (Nacht-Datum)
    val bedTime: Long,                   // Epoch millis
    val wakeTime: Long,                  // Epoch millis
    val sleepLatency: Int,               // Minuten (0-120)
    val sleepDurationMinutes: Int,       // Berechnete Schlafdauer
    val wakeDurationMinutes: Int,        // Gesamte Wachzeit
    val wakeWindows: List<WakeWindow>,   // JSON via TypeConverter
    val interruptionCount: Int,          // = wakeWindows.size
    val quality: Int,                    // 1-10
    val tags: List<String>,             // JSON via TypeConverter
    val notes: String,

    // Substanzkonsum
    val alcoholLevel: Int = 0,           // 0=nein, 1=wenig, 2=mittel, 3=viel
    val drugLevel: Int = 0,              // 0=nein, 1=wenig, 2=mittel, 3=viel
    val sleepAid: Boolean = false,       // Schlafmittel genommen?
    val medication: Boolean = false,     // Medikamente genommen?
    val medicationNotes: String = ""     // Welche Medikamente?
)
