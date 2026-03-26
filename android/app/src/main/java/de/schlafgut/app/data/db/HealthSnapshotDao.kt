package de.schlafgut.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthSnapshotDao {

    @Query("SELECT * FROM health_snapshots WHERE sleepEntryId = :sleepEntryId")
    suspend fun getForEntry(sleepEntryId: String): HealthSnapshotEntity?

    @Query(
        """SELECT hs.* FROM health_snapshots hs
           INNER JOIN sleep_entries se ON hs.sleepEntryId = se.id
           WHERE se.date BETWEEN :startDate AND :endDate"""
    )
    fun getSnapshotsInRange(startDate: Long, endDate: Long): Flow<List<HealthSnapshotEntity>>

    @Upsert
    suspend fun upsertSnapshot(snapshot: HealthSnapshotEntity)

    @Query("DELETE FROM health_snapshots WHERE sleepEntryId = :sleepEntryId")
    suspend fun deleteForEntry(sleepEntryId: String)
}
