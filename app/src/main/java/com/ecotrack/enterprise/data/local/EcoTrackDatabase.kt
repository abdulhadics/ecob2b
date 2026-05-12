package com.ecotrack.enterprise.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.data.local.dao.ReportDao
import com.ecotrack.enterprise.data.local.dao.SnapshotDao
import com.ecotrack.enterprise.data.local.entity.ActivityEntity
import com.ecotrack.enterprise.data.local.entity.CompanyEntity
import com.ecotrack.enterprise.data.local.entity.ReportEntity
import com.ecotrack.enterprise.data.local.entity.SensorSnapshotEntity
import com.ecotrack.enterprise.data.local.entity.SubscriptionEntity

@Database(
    entities = [
        SensorSnapshotEntity::class,
        ActivityEntity::class,
        ReportEntity::class,
        CompanyEntity::class,
        SubscriptionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EcoTrackDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun reportDao(): ReportDao
    abstract fun snapshotDao(): SnapshotDao
}
