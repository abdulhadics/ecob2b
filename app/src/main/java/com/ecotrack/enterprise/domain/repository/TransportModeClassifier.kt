package com.ecotrack.enterprise.domain.repository

import com.ecotrack.enterprise.domain.model.SensorSnapshot
import com.ecotrack.enterprise.domain.model.TransportMode

interface TransportModeClassifier {
    fun classify(snapshot: SensorSnapshot): TransportMode
}
