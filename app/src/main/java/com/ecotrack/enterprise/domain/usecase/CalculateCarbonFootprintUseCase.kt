package com.ecotrack.enterprise.domain.usecase

import com.ecotrack.enterprise.domain.model.TransportActivity
import com.ecotrack.enterprise.domain.model.TransportMode
import javax.inject.Inject

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : Accurate per-km CO₂ calculation   ║
 * ║  AI Reasoning Logic     : Uses DEFRA 2024 emission factors  ║
 * ║                           (UK Govt) for transport modes      ║
 * ║  Architectural Justif.  : Pure Kotlin UseCase — zero Android ║
 * ║                           dependencies, 100% unit testable   ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Emission factors sourced from UK DEFRA / BEIS 2024 guidelines.
 * Units: kg CO₂e per passenger-km
 *
 *   Stationary        → 0.0
 *   Walking / Cycling → 0.0
 *   Public Transit    → 0.089  (average bus)
 *   Car (avg petrol)  → 0.170
 *   Truck / Heavy     → 0.950  (HGV avg laden)
 */
class CalculateCarbonFootprintUseCase @Inject constructor() {

    companion object {
        private val EMISSION_FACTORS: Map<TransportMode, Float> = mapOf(
            TransportMode.STATIONARY         to 0.000f,
            TransportMode.WALKING            to 0.000f,
            TransportMode.CYCLING            to 0.000f,
            TransportMode.PUBLIC_TRANSIT     to 0.089f,
            TransportMode.CAR               to 0.170f,
            TransportMode.TRUCK_HEAVY_VEHICLE to 0.950f
        )
    }

    /**
     * Calculate CO₂ emissions for a transport activity.
     *
     * @param activity  The classified transport activity
     * @param durationSeconds  How long the activity lasted (defaults to 60s window)
     * @return Estimated CO₂ in kilograms
     */
    operator fun invoke(
        activity: TransportActivity,
        durationSeconds: Long = 60L
    ): Float {
        val factor = EMISSION_FACTORS[activity.mode] ?: 0f

        // distance (km) = speed (m/s) × duration (s) / 1000
        val distanceKm = (activity.speedMps * durationSeconds) / 1000f

        return distanceKm * factor
    }
}
