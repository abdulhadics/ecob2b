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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

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
    private val activityRepository: ActivityRepository,
    private val reportRepository: ReportRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic data sync to Supabase...")

            // Sync all pending activities from Room → Supabase
            activityRepository.syncPendingActivities()

            Log.d(TAG, "Data sync completed successfully.")
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
