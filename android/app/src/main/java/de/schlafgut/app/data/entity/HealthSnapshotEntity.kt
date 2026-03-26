package de.schlafgut.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = SleepEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["sleepEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HealthSnapshotEntity(
    @PrimaryKey val sleepEntryId: String,
    val weightKg: Double? = null,
    val bodyTempCelsius: Double? = null,
    val skinTempDeltaCelsius: Double? = null,
    val restingHeartRate: Int? = null,
    val avgNightHeartRate: Int? = null,
    val oxygenSaturation: Double? = null,
    val stepsTotal: Int? = null,
    val sleepFromDevice: String? = null,
    val fetchedAt: Long = 0
)
