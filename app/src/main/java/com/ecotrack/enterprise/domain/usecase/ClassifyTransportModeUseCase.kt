package com.ecotrack.enterprise.domain.usecase

import com.ecotrack.enterprise.domain.model.SensorSnapshot
import com.ecotrack.enterprise.domain.model.TransportActivity
import com.ecotrack.enterprise.domain.repository.TransportModeClassifier
import javax.inject.Inject

/*
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  PROMPT METADATA HEADER                                      ║
 * ║  Original Search Intent : Professional AI classification     ║
 * ║  AI Reasoning Logic     : Decoupled domain from ML tech stack║
 * ║                           via TransportModeClassifier interf.║
 * ║  Architectural Justif.  : UseCase now focuses on business    ║
 * ║                           value (mapping results) while the  ║
 * ║                           data layer handles AI heavy lifting║
 * ╚══════════════════════════════════════════════════════════════╝
 */
class ClassifyTransportModeUseCase @Inject constructor(
    private val classifier: TransportModeClassifier
) {
    operator fun invoke(snapshot: SensorSnapshot): TransportActivity {
        val mode = classifier.classify(snapshot)
        
        return TransportActivity(
            mode = mode,
            startTimestamp = snapshot.timestamp,
            speedMps = snapshot.gpsSpeedMps
        )
    }
}
