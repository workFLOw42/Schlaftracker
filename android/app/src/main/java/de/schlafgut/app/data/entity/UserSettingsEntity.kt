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
    val dosage: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val defaultSleepLatency: Int = 15,
    val appLockEnabled: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val regularMedications: List<MedicationEntry> = emptyList(),

    // Individuelle Health-Datentyp-Auswahl (Default: false – User muss explizit auswählen)
    val healthReadWeight: Boolean = false,
    val healthReadBodyTemp: Boolean = false,
    val healthReadRestingHr: Boolean = false,
    val healthReadHeartRate: Boolean = false,
    val healthReadSpO2: Boolean = false,
    val healthReadSteps: Boolean = false,
    val healthReadSleep: Boolean = false
)
