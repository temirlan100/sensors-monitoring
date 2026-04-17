package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorValue

sealed interface AlarmResult {

    data class Triggered(
        val measurement: SensorMeasurement,
        val threshold: Double
    ) : AlarmResult {
        val message: String
            get() {
                val scalar = measurement.value as? SensorValue.Scalar
                val unit = scalar?.unit ?: ""
                val amount = scalar?.amount ?: "?"
                return "ALARM: ${measurement.type.name} ${amount}${unit} " +
                        "exceeds threshold ${threshold}${unit} " +
                        "[sensor=${measurement.sensorId}, warehouse=${measurement.warehouseId}]"
            }
    }

    data class Normal(
        val measurement: SensorMeasurement
    ) : AlarmResult

    data class Skipped(
        val measurement: SensorMeasurement,
        val reason: String
    ) : AlarmResult
}
