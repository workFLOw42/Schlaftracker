package de.schlafgut.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Einzelnes Medikament mit Name und Dosierung.
 * Wird als JSON-Liste in user_settings gespeichert.
 */
@kotlinx.serialization.Serializable
data class MedicationEntry(
    val name: String,
    val dosage: String = ""  // z.B. "500mg", "2 Tabletten"
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,               // Singleton
    val userName: String = "",
    val defaultSleepLatency: Int = 15,         // Minuten
    val appLockEnabled: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val regularMedications: List<MedicationEntry> = emptyList()  // Feste Medikamente
)
