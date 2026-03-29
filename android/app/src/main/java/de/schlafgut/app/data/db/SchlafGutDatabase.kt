package de.schlafgut.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.UserSettingsEntity

@Database(
    entities = [
        SleepEntryEntity::class,
        UserSettingsEntity::class,
        HealthSnapshotEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SchlafGutDatabase : RoomDatabase() {
    abstract fun sleepEntryDao(): SleepEntryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun healthSnapshotDao(): HealthSnapshotDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_entries ADD COLUMN alcoholLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_entries ADD COLUMN drugLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_entries ADD COLUMN sleepAid INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_entries ADD COLUMN medication INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_entries ADD COLUMN medicationNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Feste Medikamenten-Liste im Profil
                db.execSQL("ALTER TABLE user_settings ADD COLUMN regularMedications TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
