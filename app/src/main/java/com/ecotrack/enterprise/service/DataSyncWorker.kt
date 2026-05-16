package com.ecotrack.enterprise.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ecotrack.enterprise.data.repository.ActivityRepository
import com.ecotrack.enterprise.data.repository.ReportRepository
import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.service.SyncSchedulerService
import com.ecotrack.enterprise.service.SyncPacket
import com.ecotrack.enterprise.service.SyncPriority
import com.ecotrack.enterprise.service.VectorClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : Automatic background cloud sync    ║
 * ║  AI Reasoning Logic     : WorkManager survives app kills &   ║
 * ║                           Doze mode — ideal for periodic     ║
 * ║                           batch uploads of offline data      ║
 * ║  Architectural Justif.  : Decouples sync from UI lifecycle;  ║
 * ║                           runs even when app is not open     ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val activityDao: ActivityDao,
    private val syncScheduler: SyncSchedulerService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Fetching pending activities for EDF scheduling...")
            val pending = activityDao.getPendingActivities()
            
            for (activity in pending) {
                val packet = SyncPacket(
                    id = activity.id,
                    timestampMs = activity.startTimestampMs,
                    sizeBytes = 256, // Average activity JSON size
                    priority = if (activity.syncPriority == "HIGH") SyncPriority.HIGH else SyncPriority.LOW,
                    ttlMs = 7200000, // 2 hour default TTL
                    vectorClock = VectorClock()
                )
                syncScheduler.addPacket(packet)
            }

            Log.d(TAG, "Executing Sync via Scheduler Engine...")
            syncScheduler.executeSync(isWifiStable = true) // In production, use NetworkMonitor

            Log.d(TAG, "Data sync completed via EDF strategy.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Data sync failed, will retry.", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DataSyncWorker"
        private const val WORK_NAME = "ecotrack_periodic_sync"

        /**
         * Schedule a periodic sync every 15 minutes.
         * WorkManager guarantees this runs even if the app is killed.
         */
        fun schedule(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Periodic sync scheduled (every 15 min).")
        }
    }
}
