package com.ecotrack.enterprise.di

import android.content.Context
import androidx.room.Room
import com.ecotrack.enterprise.data.local.EcoTrackDatabase
import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.data.local.dao.ReportDao
import com.ecotrack.enterprise.data.local.dao.SnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEcoTrackDatabase(
        @ApplicationContext context: Context
    ): EcoTrackDatabase {
        return Room.databaseBuilder(
            context,
            EcoTrackDatabase::class.java,
            "ecotrack_db"
        ).build()
    }

    @Provides
    fun provideActivityDao(database: EcoTrackDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    fun provideReportDao(database: EcoTrackDatabase): ReportDao {
        return database.reportDao()
    }

    @Provides
    fun provideSnapshotDao(database: EcoTrackDatabase): SnapshotDao {
        return database.snapshotDao()
    }
}
