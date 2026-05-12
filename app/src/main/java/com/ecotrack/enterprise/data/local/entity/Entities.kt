package com.ecotrack.enterprise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : High-resolution offline-first DB   ║
 * ║  AI Reasoning Logic     : Normalized schema separates raw    ║
 * ║                           sensor data from computed results  ║
 * ║  Architectural Justif.  : Supports legal audit trail with    ║
 * ║                           immutable timestamped records       ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

@Entity(tableName = "sensor_snapshots")
data class SensorSnapshotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val gpsSpeedMps: Float,
    val gpsLat: Double,
    val gpsLon: Double,
    val accelVariance: Float,
    val isSynced: Boolean = false
)

@Serializable
@Entity(tableName = "transport_activities")
data class ActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @kotlinx.serialization.SerialName("transportmode") val transportMode: String,
    @kotlinx.serialization.SerialName("starttimestampms") val startTimestampMs: Long,
    @kotlinx.serialization.SerialName("endtimestampms") val endTimestampMs: Long,
    @kotlinx.serialization.SerialName("distancemeters") val distanceMeters: Float,
    @kotlinx.serialization.SerialName("avgspeedmps") val avgSpeedMps: Float,
    @kotlinx.serialization.SerialName("co2kgemitted") val co2KgEmitted: Float,
    @kotlinx.serialization.SerialName("companyid") val companyId: String,
    @kotlinx.serialization.SerialName("isauditverified") val isAuditVerified: Boolean = false,
    @kotlinx.serialization.SerialName("user_id") val user_id: String? = null // New field to match SQL
)

@Serializable
@Entity(tableName = "sustainability_reports")
data class ReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @kotlinx.serialization.SerialName("companyid") val companyId: String,
    @kotlinx.serialization.SerialName("reportperiodstart") val reportPeriodStart: Long,
    @kotlinx.serialization.SerialName("reportperiodend") val reportPeriodEnd: Long,
    @kotlinx.serialization.SerialName("totalco2kg") val totalCo2Kg: Float,
    @kotlinx.serialization.SerialName("offsetpurchased") val offsetPurchased: Float,
    @kotlinx.serialization.SerialName("netco2kg") val netCo2Kg: Float,
    @kotlinx.serialization.SerialName("executivesummaryjson") val executiveSummaryJson: String,
    @kotlinx.serialization.SerialName("pdfblobpath") val pdfBlobPath: String?,
    @kotlinx.serialization.SerialName("createdat") val createdAt: Long = System.currentTimeMillis(),
    @kotlinx.serialization.SerialName("islegallysubmitted") val isLegallySubmitted: Boolean = false,
    @kotlinx.serialization.SerialName("user_id") val user_id: String? = null // New field to match SQL
)

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val industry: String,            // e.g., "Logistics", "Manufacturing"
    val employeeCount: Int,
    val adminEmail: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val plan: String,                // "starter", "professional", "enterprise"
    val creditsTotal: Int,
    val creditsUsed: Int = 0,
    val pricePerMonth: Float,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true
)
