package de.schlafgut.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.schlafgut.app.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: UserSettingsEntity)
}
