package com.ecotrack.enterprise.data.ml

import android.content.Context
import com.ecotrack.enterprise.domain.model.SensorSnapshot
import com.ecotrack.enterprise.domain.model.TransportMode
import com.ecotrack.enterprise.domain.repository.TransportModeClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PytorchTransportClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : TransportModeClassifier {

    private var module: Module? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // Check if model exists in assets first
            val assetName = "transport_model.ptl"
            val file = File(context.filesDir, assetName)
            if (!file.exists()) {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            module = LiteModuleLoader.load(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Model not found or failed to load
        }
    }

    override fun classify(snapshot: SensorSnapshot): TransportMode {
        val model = module ?: return fallbackLogic(snapshot)

        return try {
            // 1. Prepare input tensor (Example: [speed, variance, lat, lon])
            // In a real scenario, you'd have a window of data, but here we use the snapshot
            val inputData = floatArrayOf(
                snapshot.gpsSpeedMps,
                snapshot.accelVariance,
                snapshot.gpsLat.toFloat(),
                snapshot.gpsLon.toFloat()
            )
            val inputTensor = Tensor.fromBlob(inputData, longArrayOf(1, 4))

            // 2. Run inference
            val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray

            // 3. Find max score index
            val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

            // 4. Map index to TransportMode
            when (maxIndex) {
                0 -> TransportMode.IDLE
                1 -> TransportMode.WALKING
                2 -> TransportMode.TRANSIT
                3 -> TransportMode.TRANSIT
                4 -> TransportMode.TRANSIT
                5 -> TransportMode.HEAVY_VEHICLE
                else -> fallbackLogic(snapshot)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackLogic(snapshot)
        }
    }

    /**
     * Fallback to rule-based logic if the AI model is unavailable or fails.
     */
    private fun fallbackLogic(snapshot: SensorSnapshot): TransportMode {
        return when {
            snapshot.gpsSpeedMps < 0.5f && snapshot.accelVariance < 0.1f ->
                TransportMode.IDLE
            snapshot.gpsSpeedMps < 2.0f && snapshot.accelVariance in 0.1f..0.8f ->
                TransportMode.WALKING
            snapshot.gpsSpeedMps < 7.0f && snapshot.accelVariance in 0.4f..1.5f ->
                TransportMode.TRANSIT
            snapshot.gpsSpeedMps < 15.0f ->
                TransportMode.TRANSIT
            snapshot.accelVariance < 0.3f ->
                TransportMode.TRANSIT
            else -> TransportMode.HEAVY_VEHICLE
        }
    }
}
