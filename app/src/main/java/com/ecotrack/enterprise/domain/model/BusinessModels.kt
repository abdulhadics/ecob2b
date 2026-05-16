package com.ecotrack.enterprise.domain.model

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : B2B SaaS Monetization Schema       ║
 * ║  AI Reasoning Logic     : Decouples subscription (access)    ║
 * ║                           from credits (usage-based billing)  ║
 * ║  Architectural Justif.  : Immutable data classes for domain   ║
 * ║                           integrity and thread safety        ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

enum class SubscriptionTier {
    FREE,           // Individual tracking
    STARTER_B2B,    // Small teams, basic reports
    ENTERPRISE,     // Full fleet tracking, audit-ready reports
    AUDITOR         // Multi-company access, verification tools
}

data class UserSubscription(
    val id: String,
    val userId: String,
    val companyId: String?,
    val tier: SubscriptionTier,
    val expiryTimestamp: Long,
    val isActive: Boolean
)

data class CreditBalance(
    val companyId: String,
    val trackingCredits: Long, // 1 credit = 10km of AI verification
    val reportCredits: Int     // 1 credit = 1 certified ESG report
)

data class ESGReport(
    val id: String,
    val companyId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalDistanceKm: Double,
    val totalCo2Kg: Double,
    val offsetCreditsRequired: Double,
    val confidenceScore: Float, // AI confidence in data accuracy
    val status: ReportStatus
)

enum class ReportStatus {
    DRAFT,
    CERTIFIED,
    SUBMITTED_TO_REGULATOR
}
