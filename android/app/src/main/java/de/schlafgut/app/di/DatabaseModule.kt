package de.schlafgut.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.schlafgut.app.data.db.HealthSnapshotDao
import de.schlafgut.app.data.db.SchlafGutDatabase
import de.schlafgut.app.data.db.SettingsDao
import de.schlafgut.app.data.db.SleepEntryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SchlafGutDatabase =
        Room.databaseBuilder(
            context,
            SchlafGutDatabase::class.java,
            "schlafgut.db"
        )
            .addMigrations(
                SchlafGutDatabase.MIGRATION_1_2,
                SchlafGutDatabase.MIGRATION_2_3
            )
            .build()

    @Provides
    fun provideSleepEntryDao(db: SchlafGutDatabase): SleepEntryDao =
        db.sleepEntryDao()

    @Provides
    fun provideSettingsDao(db: SchlafGutDatabase): SettingsDao =
        db.settingsDao()

    @Provides
    fun provideHealthSnapshotDao(db: SchlafGutDatabase): HealthSnapshotDao =
        db.healthSnapshotDao()
}
