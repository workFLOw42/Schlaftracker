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
import de.schlafgut.app.data.entity.UserSettingsEntity
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    /**
     * Liest Health-Daten und erstellt einen Snapshot.
     *
     * Zeitraum-Strategie:
     * - Gewicht: Immer den letzten verfügbaren Wert (kein Zeitlimit)
     * - Körpertemp: Letzter Wert, max 24h alt
     * - Ruhepuls: Letzter Wert der letzten 7 Tage
     * - Herzfrequenz: Durchschnitt während der Schlafzeit
     * - SpO₂: Letzter Wert der letzten 7 Tage
     * - Schritte: Summe des Tages vor dem Schlaf
     */
    suspend fun fetchHealthSnapshot(
        sleepEntryId: String,
        bedTimeEpoch: Long,
        wakeTimeEpoch: Long,
        settings: UserSettingsEntity = UserSettingsEntity()
    ): HealthSnapshotEntity? {
        val client = healthConnectManager.getClient() ?: return null

        val bedInstant = Instant.ofEpochMilli(bedTimeEpoch)
        val wakeInstant = Instant.ofEpochMilli(wakeTimeEpoch)
        val now = Instant.now()

        // Gewicht: Letzter Wert aller Zeiten (max 365 Tage zurück – HC-Limit)
        val allTime = TimeRangeFilter.between(now.minusSeconds(365L * 24 * 3600), now)
        // Körpertemp: Max 24h alt
        val last24h = TimeRangeFilter.between(now.minusSeconds(24L * 3600), now)
        // Ruhepuls, SpO₂: Letzte 7 Tage
        val last7Days = TimeRangeFilter.between(now.minusSeconds(7L * 24 * 3600), now)
        // Herzfrequenz: Während der Schlafzeit
        val sleepRange = TimeRangeFilter.between(bedInstant, wakeInstant)
        // Schritte: Ganzer Tag des Schlafens
        val bedDate = bedInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        val dayStart = bedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayEnd = bedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayRange = TimeRangeFilter.between(dayStart, dayEnd)

        val weight = if (settings.healthReadWeight) readLastWeight(client, allTime) else null
        val bodyTemp = if (settings.healthReadBodyTemp) readLastBodyTemp(client, last24h) else null
        val restingHr = if (settings.healthReadRestingHr) readRestingHeartRate(client, last7Days) else null
        val avgNightHr = if (settings.healthReadHeartRate) readAverageHeartRate(client, sleepRange) else null
        val spo2 = if (settings.healthReadSpO2) readLastOxygenSaturation(client, last7Days) else null
        val steps = if (settings.healthReadSteps) readTotalSteps(client, dayRange) else null

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

    private suspend fun readLastWeight(client: HealthConnectClient, range: TimeRangeFilter): Double? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = range))
        response.records.lastOrNull()?.weight?.inKilograms
    }

    private suspend fun readLastBodyTemp(client: HealthConnectClient, range: TimeRangeFilter): Double? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, timeRangeFilter = range))
        response.records.lastOrNull()?.temperature?.inCelsius
    }

    private suspend fun readRestingHeartRate(client: HealthConnectClient, range: TimeRangeFilter): Int? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = range))
        response.records.lastOrNull()?.beatsPerMinute?.toInt()
    }

    private suspend fun readAverageHeartRate(client: HealthConnectClient, range: TimeRangeFilter): Int? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range))
        val allSamples = response.records.flatMap { it.samples }
        if (allSamples.isEmpty()) null else allSamples.map { it.beatsPerMinute }.average().toInt()
    }

    private suspend fun readLastOxygenSaturation(client: HealthConnectClient, range: TimeRangeFilter): Double? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter = range))
        response.records.lastOrNull()?.percentage?.value
    }

    private suspend fun readTotalSteps(client: HealthConnectClient, range: TimeRangeFilter): Int? = tryRead {
        val response = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = range))
        if (response.records.isEmpty()) null else response.records.sumOf { it.count }.toInt()
    }

    private suspend fun <T> tryRead(block: suspend () -> T?): T? {
        return try {
            block()
        } catch (_: Exception) {
            null
        }
    }
}
