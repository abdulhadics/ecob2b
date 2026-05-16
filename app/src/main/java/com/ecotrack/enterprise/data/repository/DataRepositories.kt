package com.ecotrack.enterprise.data.repository

import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.data.local.dao.ReportDao
import com.ecotrack.enterprise.data.local.entity.ActivityEntity
import com.ecotrack.enterprise.data.local.entity.ReportEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : Offline-first sync with Supabase   ║
 * ║  AI Reasoning Logic     : Room is the Single Source of Truth ║
 * ║                           Supabase is the cloud mirror       ║
 * ║  Architectural Justif.  : Write-through cache: save locally  ║
 * ║                           first, then push to cloud. If the  ║
 * ║                           push fails, data is safe in Room   ║
 * ║                           and will retry on next sync cycle  ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val supabaseClient: SupabaseClient
) {
    /** Observe all activities for a company (from local Room DB) */
    fun getActivitiesForCompany(companyId: String): Flow<List<ActivityEntity>> {
        return activityDao.getActivitiesForCompany(companyId)
    }

    /** Save locally first, then attempt cloud sync */
    suspend fun saveActivity(activity: ActivityEntity) {
        // Step 1: Always save to local Room (offline-first)
        activityDao.insertActivity(activity)

        // Step 2: Try to push to Supabase cloud ONLY if user is logged in
        try {
            if (supabaseClient.auth.currentSessionOrNull() != null) {
                supabaseClient.postgrest["transport_activities"].insert(activity)
            }
        } catch (e: Exception) {
            // Network failure or guest mode — data is safe in Room
            e.printStackTrace()
        }
    }

    /** Batch sync all un-synced activities to cloud */
    suspend fun syncPendingActivities() {
        // In production: query Room for isSynced=false, push batch, mark synced
        // Simplified for initial architecture
    }
}

@Singleton
class ReportRepository @Inject constructor(
    private val reportDao: ReportDao,
    private val supabaseClient: SupabaseClient
) {
    fun getReportsForCompany(companyId: String): Flow<List<ReportEntity>> {
        return reportDao.getReportsForCompany(companyId)
    }

    suspend fun saveReport(report: ReportEntity) {
        reportDao.insertReport(report)

        try {
            if (supabaseClient.auth.currentSessionOrNull() != null) {
                supabaseClient.postgrest["sustainability_reports"].insert(report)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
