package de.schlafgut.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import de.schlafgut.app.data.entity.SleepEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepEntryDao {

    @Query("SELECT * FROM sleep_entries ORDER BY bedTime DESC")
    fun getAllEntries(): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries WHERE id = :id")
    suspend fun getEntryById(id: String): SleepEntryEntity?

    @Query(
        "SELECT * FROM sleep_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY bedTime DESC"
    )
    fun getEntriesInRange(startDate: Long, endDate: Long): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries ORDER BY bedTime DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries ORDER BY bedTime DESC LIMIT :limit")
    suspend fun getRecentEntriesOnce(limit: Int): List<SleepEntryEntity>

    @Query("SELECT COUNT(*) FROM sleep_entries")
    fun getEntryCount(): Flow<Int>

    @Upsert
    suspend fun upsertEntry(entry: SleepEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM sleep_entries")
    suspend fun deleteAllEntries()

    @Query(
        """SELECT * FROM sleep_entries
           WHERE id != :excludeId
           AND ((bedTime <= :wakeTime AND wakeTime >= :bedTime))"""
    )
    suspend fun findOverlappingEntries(
        bedTime: Long,
        wakeTime: Long,
        excludeId: String
    ): List<SleepEntryEntity>
}
