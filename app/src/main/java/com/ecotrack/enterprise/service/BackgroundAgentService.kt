package com.ecotrack.enterprise.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.data.local.entity.ActivityEntity
import com.ecotrack.enterprise.domain.usecase.CalculateCarbonFootprintUseCase
import com.ecotrack.enterprise.domain.usecase.ClassifyTransportModeUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : Passive transport mode detection   ║
 * ║                           without user input on Android 14+  ║
 * ║  AI Reasoning Logic     : Fuse GPS velocity + accelerometer  ║
 * ║                           variance to classify modes with    ║
 * ║                           a lightweight on-device ML model   ║
 * ║  Architectural Justif.  : ForegroundService + WorkManager    ║
 * ║                           combo survives Doze mode & battery ║
 * ║                           optimization on Android 14         ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class BackgroundAgentService : Service() {

    @Inject lateinit var sensorFusionEngine: SensorFusionEngine
    @Inject lateinit var classifyTransportMode: ClassifyTransportModeUseCase
    @Inject lateinit var calculateCarbon: CalculateCarbonFootprintUseCase
    @Inject lateinit var activityDao: ActivityDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildPersistentNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        launchSensorPipeline()
        sensorFusionEngine.startListening()
        return START_STICKY
    }

    private fun launchSensorPipeline() {
        serviceScope.launch {
            sensorFusionEngine.sensorDataFlow
                .buffer(capacity = 64)
                .map { snapshot -> classifyTransportMode(snapshot) }
                .distinctUntilChanged()
                .collect { activity ->
                    val emission = calculateCarbon(activity)
                    
                    val entity = ActivityEntity(
                        transportMode = activity.mode.name,
                        startTimestampMs = activity.startTimestamp,
                        endTimestampMs = System.currentTimeMillis(),
                        distanceMeters = 0f,
                        avgSpeedMps = activity.speedMps,
                        co2KgEmitted = emission,
                        companyId = "company_123"
                    )
                    activityDao.insertActivity(entity)
                }
        }
    }

    private fun buildPersistentNotification(): Notification {
        val channelId = "ecotrack_agent_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("EcoTrack Active")
            .setContentText("Passively monitoring transport mode")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<AgentRestartWorker>().build()
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

class AgentRestartWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val intent = Intent(applicationContext, BackgroundAgentService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        return Result.success()
    }
}
