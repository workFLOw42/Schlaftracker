package de.schlafgut.app.data.repository

import de.schlafgut.app.data.db.HealthSnapshotDao
import de.schlafgut.app.data.db.SleepEntryDao
import de.schlafgut.app.data.db.SettingsDao
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepository @Inject constructor(
    private val sleepEntryDao: SleepEntryDao,
    private val settingsDao: SettingsDao,
    private val healthSnapshotDao: HealthSnapshotDao
) {
    // --- Sleep Entries ---

    fun getAllEntries(): Flow<List<SleepEntryEntity>> =
        sleepEntryDao.getAllEntries()

    fun getRecentEntries(limit: Int = 5): Flow<List<SleepEntryEntity>> =
        sleepEntryDao.getRecentEntries(limit)

    fun getEntriesInRange(startDate: Long, endDate: Long): Flow<List<SleepEntryEntity>> =
        sleepEntryDao.getEntriesInRange(startDate, endDate)

    fun getEntryCount(): Flow<Int> =
        sleepEntryDao.getEntryCount()

    suspend fun getEntryById(id: String): SleepEntryEntity? =
        sleepEntryDao.getEntryById(id)

    suspend fun saveEntry(entry: SleepEntryEntity) =
        sleepEntryDao.upsertEntry(entry)

    suspend fun deleteEntry(id: String) =
        sleepEntryDao.deleteEntryById(id)

    suspend fun deleteAllEntries() =
        sleepEntryDao.deleteAllEntries()

    suspend fun findOverlappingEntries(
        bedTime: Long,
        wakeTime: Long,
        excludeId: String
    ): List<SleepEntryEntity> =
        sleepEntryDao.findOverlappingEntries(bedTime, wakeTime, excludeId)

    // --- Statistics helpers ---

    fun getAverageQuality(): Flow<Double?> =
        sleepEntryDao.getAllEntries().map { entries ->
            if (entries.isEmpty()) null
            else entries.map { it.quality }.average()
        }

    fun getAverageDuration(): Flow<Double?> =
        sleepEntryDao.getAllEntries().map { entries ->
            if (entries.isEmpty()) null
            else entries.map { it.sleepDurationMinutes }.average()
        }

    // --- Settings ---

    fun getSettings(): Flow<UserSettingsEntity?> =
        settingsDao.getSettings()

    suspend fun getSettingsOnce(): UserSettingsEntity =
        settingsDao.getSettingsOnce() ?: UserSettingsEntity()

    suspend fun saveSettings(settings: UserSettingsEntity) =
        settingsDao.upsertSettings(settings)

    // --- Health Snapshots ---

    suspend fun getHealthSnapshot(sleepEntryId: String): HealthSnapshotEntity? =
        healthSnapshotDao.getForEntry(sleepEntryId)

    fun getHealthSnapshotsInRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<HealthSnapshotEntity>> =
        healthSnapshotDao.getSnapshotsInRange(startDate, endDate)

    suspend fun saveHealthSnapshot(snapshot: HealthSnapshotEntity) =
        healthSnapshotDao.upsertSnapshot(snapshot)
}
