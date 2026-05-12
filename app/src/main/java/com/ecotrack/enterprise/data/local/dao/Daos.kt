package com.ecotrack.enterprise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecotrack.enterprise.data.local.entity.ActivityEntity
import com.ecotrack.enterprise.data.local.entity.ReportEntity
import com.ecotrack.enterprise.data.local.entity.SensorSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SensorSnapshotEntity)

    @Query("SELECT * FROM sensor_snapshots ORDER BY timestampMs DESC LIMIT 100")
    fun getRecentSnapshots(): Flow<List<SensorSnapshotEntity>>
}

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Query("SELECT * FROM transport_activities WHERE companyId = :companyId")
    fun getActivitiesForCompany(companyId: String): Flow<List<ActivityEntity>>
}

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("SELECT * FROM sustainability_reports WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun getReportsForCompany(companyId: String): Flow<List<ReportEntity>>
}
