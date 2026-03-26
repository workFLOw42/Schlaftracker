package de.schlafgut.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity

@Database(
    entities = [
        SleepEntryEntity::class,
        UserSettingsEntity::class,
        HealthSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SchlafGutDatabase : RoomDatabase() {
    abstract fun sleepEntryDao(): SleepEntryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun healthSnapshotDao(): HealthSnapshotDao
}
