package com.ecotrack.enterprise.service

import com.ecotrack.enterprise.domain.model.SensorReading
import com.ecotrack.enterprise.domain.model.TransportActivity
import com.ecotrack.enterprise.domain.model.TransportMode
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/*
 ═══════════════════════════════════════════════════════
 EcoTrack Enterprise | DAA Algorithmic Module Header
 ═══════════════════════════════════════════════════════
 [ALGORITHMIC INTENT]
   → Classify noisy GPS streams into discrete transport modes using a 
     sliding window majority-vote smoothing technique to eliminate transients.

 [PARADIGM & DATA STRUCTURE]
   → Streaming Sliding Window over ArrayDeque — chosen over full-buffer 
     re-scanning because it provides amortized O(1) updates. A companion 
     Frequency HashMap maintains counts incrementally on enqueue/dequeue.

 [FORMAL COMPLEXITY PROOF]
   → Worst-Case: O(1) per update - HashMap put/get and Deque add/remove are O(1).
   → Best-Case: O(1).
   → Average-Case: O(1).
   → Recurrence Relation: T(n) = T(n-1) + O(1) for n updates.
   → Space Complexity: O(K + M) where K is window size and M is the number 
     of transport modes. Since M is constant (<= 5), total space is O(K).

 [FAILURE & EDGE CASE ANALYSIS]
   → GPS signal lost (>30s): The algorithm detects null coordinates/dropout, 
     freezes window updates, and emits the last majority mode with 0.0 confidence.
   → Tie between modes: Defer to the most recent PyTorch Lite model output 
     to maintain temporal relevance.
   → K=1 (Degenerate): Return the single reading directly with O(1) complexity.

 [BUSINESS & SUSTAINABILITY UTILITY]
   → Amortized O(1) updates ensure zero processing lag during fleet peak 
     hours, minimizing CPU cycles and maximizing battery life for ESG 
     compliance tracking.
 ═══════════════════════════════════════════════════════
*/

@Singleton
class TrackingEngine @Inject constructor(
    private var K: Int = 10
) {
    private val window = ArrayDeque<SensorReading>(K)
    private val modeFrequencies = HashMap<TransportMode, Int>()
    
    private var lastValidMode: TransportMode = TransportMode.IDLE
    private var lastReadingTime: Long = 0
    private val DROPOUT_THRESHOLD_MS = 30000L

    /**
     * Processes a new reading and returns smoothed activity.
     * Amortized O(1) complexity.
     */
    fun processReading(reading: SensorReading): TransportActivity {
        val currentTime = System.currentTimeMillis()
        
        // Edge Case: GPS Dropout (null coordinates or >30s gap)
        val isDropout = reading.gpsLat == null || reading.gpsLon == null || 
                        (lastReadingTime != 0L && currentTime - lastReadingTime > DROPOUT_THRESHOLD_MS)
        
        if (isDropout) {
            return TransportActivity(
                mode = lastValidMode,
                startTimestamp = reading.timestamp,
                speedMps = reading.gpsSpeedMps,
                confidence = 0.0f
            )
        }
        
        lastReadingTime = currentTime

        // Edge Case: K=1 (Degenerate window)
        if (K <= 1) {
            lastValidMode = reading.mode
            return TransportActivity(
                mode = reading.mode,
                startTimestamp = reading.timestamp,
                speedMps = reading.gpsSpeedMps,
                confidence = 1.0f
            )
        }

        // Sliding window management: O(1) amortized
        if (window.size >= K) {
            val evicted = window.removeFirst()
            val count = modeFrequencies[evicted.mode] ?: 1
            if (count <= 1) {
                modeFrequencies.remove(evicted.mode)
            } else {
                modeFrequencies[evicted.mode] = count - 1
            }
        }

        window.addLast(reading)
        modeFrequencies[reading.mode] = (modeFrequencies[reading.mode] ?: 0) + 1

        // Majority vote: O(M) where M is number of modes (constant <= 5)
        var maxCount = -1
        var winningMode = reading.mode
        var isTie = false

        for ((mode, count) in modeFrequencies) {
            if (count > maxCount) {
                maxCount = count
                winningMode = mode
                isTie = false
            } else if (count == maxCount) {
                isTie = true
            }
        }

        // Edge Case: Tie - Defer to PyTorch Lite model output (current reading's mode)
        if (isTie) {
            winningMode = reading.mode
        }

        lastValidMode = winningMode
        
        return TransportActivity(
            mode = winningMode,
            startTimestamp = reading.timestamp,
            speedMps = reading.gpsSpeedMps,
            confidence = confidenceScore()
        )
    }

    /**
     * Calculates confidence as the ratio of winning mode count to window size.
     */
    fun confidenceScore(): Float {
        if (window.isEmpty()) return 0.0f
        val winningModeCount = modeFrequencies[lastValidMode] ?: 0
        return winningModeCount.toFloat() / window.size
    }

    /**
     * Reconfigures window size K.
     */
    fun configureWindow(newK: Int) {
        if (newK < 1) return
        K = newK
        window.clear()
        modeFrequencies.clear()
    }
}
