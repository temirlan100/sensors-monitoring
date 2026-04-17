package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorValue
import com.beamtrail.monitoring.config.ThresholdConfig
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ThresholdEvaluator(
    private val config: ThresholdConfig
) {

    fun evaluate(measurement: SensorMeasurement): AlarmResult {
        val value = measurement.value
        if (value !is SensorValue.Scalar) {
            return AlarmResult.Skipped(measurement, "Non-scalar value, threshold check not applicable")
        }

        val threshold = config.sensors()[measurement.type.name]
            ?: return AlarmResult.Skipped(measurement, "No threshold configured for '${measurement.type.name}'")

        return if (value.amount > threshold) {
            AlarmResult.Triggered(measurement, threshold)
        } else {
            AlarmResult.Normal(measurement)
        }
    }
}
