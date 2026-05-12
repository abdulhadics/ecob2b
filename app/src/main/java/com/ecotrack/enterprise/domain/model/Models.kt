package com.ecotrack.enterprise.domain.model

data class SensorSnapshot(
    val timestamp: Long,
    val gpsSpeedMps: Float,
    val gpsLat: Double,
    val gpsLon: Double,
    val accelVariance: Float,
    val rawAccel: List<FloatArray>
)

enum class TransportMode {
    STATIONARY, WALKING, CYCLING, PUBLIC_TRANSIT, CAR, TRUCK_HEAVY_VEHICLE
}

data class TransportActivity(
    val mode: TransportMode,
    val startTimestamp: Long,
    val speedMps: Float
)
