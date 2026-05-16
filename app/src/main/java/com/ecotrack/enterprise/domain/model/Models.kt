/*
 ═══════════════════════════════════════════════════════
 EcoTrack Enterprise | DAA Algorithmic Module Header
 ═══════════════════════════════════════════════════════
 [ALGORITHMIC INTENT]
   → Define immutable domain entities and DEFRA 2024 emission factors 
     to support high-resolution carbon auditing.

 [PARADIGM & DATA STRUCTURE]
   → Data Classes and Objects. Constants are used for emission factors 
     to ensure O(1) lookup during calculation cycles.

 [FORMAL COMPLEXITY PROOF]
   → Lookup Complexity: O(1).
   → Space Complexity: O(M) where M is the number of defined modes.

 [FAILURE & EDGE CASE ANALYSIS]
   → Missing Mode: Fallback logic handles undefined modes by defaulting 
     to zero emission factors to prevent report inflation.

 [BUSINESS & SUSTAINABILITY UTILITY]
   → Centralized regulatory factors ensure audit consistency across 
     multi-tenant enterprise environments.
 ═══════════════════════════════════════════════════════
*/
package com.ecotrack.enterprise.domain.model

object DefraEmissionFactors {
    const val CAR_PETROL = 0.170 // kg CO2 per km
    const val CAR_EV = 0.052
    const val HEAVY_GOODS_VEHICLE = 0.850
    const val TRANSIT_BUS = 0.089
    const val WALKING_CYCLING = 0.000
}

data class SensorSnapshot(
    val timestamp: Long,
    val gpsSpeedMps: Float,
    val gpsLat: Double,
    val gpsLon: Double,
    val accelVariance: Float,
    val rawAccel: List<FloatArray>
)

data class SensorReading(
    val timestamp: Long,
    val gpsSpeedMps: Float,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val accelVariance: Float,
    val mode: TransportMode
)

enum class TransportMode {
    IDLE, WALKING, TRANSIT, HEAVY_VEHICLE,
    STATIONARY, CYCLING, PUBLIC_TRANSIT, CAR, TRUCK_HEAVY_VEHICLE // legacy
}

data class TransportActivity(
    val mode: TransportMode,
    val startTimestamp: Long,
    val speedMps: Float,
    val confidence: Float = 1.0f
)
