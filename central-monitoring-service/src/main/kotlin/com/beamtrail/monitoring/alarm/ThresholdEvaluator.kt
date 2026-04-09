package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorType
import com.beamtrail.monitoring.config.ThresholdConfig
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ThresholdEvaluator(
    private val config: ThresholdConfig
) {

    fun evaluate(measurement: SensorMeasurement): AlarmResult {
        val threshold = when (measurement.type) {
            SensorType.TEMPERATURE -> config.temperature()
            SensorType.HUMIDITY -> config.humidity()
        }

        return if (measurement.value > threshold) {
            AlarmResult.Triggered(measurement, threshold)
        } else {
            AlarmResult.Normal(measurement)
        }
    }
}
