package de.schlafgut.app.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    /**
     * Liest Health-Daten für einen Schlafzeitraum und erstellt einen Snapshot.
     * Liest:
     * - Gewicht: letzter Wert des Tages vor dem Schlaf
     * - Körpertemperatur: letzter Wert
     * - Ruhepuls: Wert des Tages
     * - Herzfrequenz: Durchschnitt während der Schlafzeit
     * - SpO2: letzter Wert
     * - Schritte: Summe des Tages
     */
    suspend fun fetchHealthSnapshot(
        sleepEntryId: String,
        bedTimeEpoch: Long,
        wakeTimeEpoch: Long
    ): HealthSnapshotEntity? {
        val client = healthConnectManager.getClient() ?: return null

        val bedInstant = Instant.ofEpochMilli(bedTimeEpoch)
        val wakeInstant = Instant.ofEpochMilli(wakeTimeEpoch)

        // Day range: from start of bed day to end of wake day
        val dayStart = bedInstant.minusSeconds(12 * 3600) // ~12h before bed
        val dayEnd = wakeInstant.plusSeconds(6 * 3600) // ~6h after wake

        val sleepRange = TimeRangeFilter.between(bedInstant, wakeInstant)
        val dayRange = TimeRangeFilter.between(dayStart, dayEnd)

        val weight = readLastWeight(client, dayRange)
        val bodyTemp = readLastBodyTemp(client, dayRange)
        val restingHr = readRestingHeartRate(client, dayRange)
        val avgNightHr = readAverageHeartRate(client, sleepRange)
        val spo2 = readLastOxygenSaturation(client, sleepRange)
        val steps = readTotalSteps(client, dayRange)

        // Only create snapshot if at least one value is present
        if (weight == null && bodyTemp == null && restingHr == null &&
            avgNightHr == null && spo2 == null && steps == null
        ) {
            return null
        }

        return HealthSnapshotEntity(
            sleepEntryId = sleepEntryId,
            weightKg = weight,
            bodyTempCelsius = bodyTemp,
            restingHeartRate = restingHr,
            avgNightHeartRate = avgNightHr,
            oxygenSaturation = spo2,
            stepsTotal = steps,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private suspend fun readLastWeight(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Double? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(WeightRecord::class, timeRangeFilter = range)
        )
        response.records.lastOrNull()?.weight?.inKilograms
    }

    private suspend fun readLastBodyTemp(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Double? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(BodyTemperatureRecord::class, timeRangeFilter = range)
        )
        response.records.lastOrNull()?.temperature?.inCelsius
    }

    private suspend fun readRestingHeartRate(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Int? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = range)
        )
        response.records.lastOrNull()?.beatsPerMinute?.toInt()
    }

    private suspend fun readAverageHeartRate(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Int? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range)
        )
        val allSamples = response.records.flatMap { it.samples }
        if (allSamples.isEmpty()) null
        else allSamples.map { it.beatsPerMinute }.average().toInt()
    }

    private suspend fun readLastOxygenSaturation(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Double? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter = range)
        )
        response.records.lastOrNull()?.percentage?.value
    }

    private suspend fun readTotalSteps(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): Int? = tryRead {
        val response = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = range)
        )
        if (response.records.isEmpty()) null
        else response.records.sumOf { it.count }.toInt()
    }

    private suspend fun <T> tryRead(block: suspend () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            null // Permission denied or HC unavailable
        }
    }
}
