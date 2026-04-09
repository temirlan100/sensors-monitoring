package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorMeasurement

sealed interface AlarmResult {

    data class Triggered(
        val measurement: SensorMeasurement,
        val threshold: Double
    ) : AlarmResult {
        val message: String
            get() = "ALARM: ${measurement.type} ${measurement.value}${measurement.type.unit} " +
                    "exceeds threshold ${threshold}${measurement.type.unit} " +
                    "[sensor=${measurement.sensorId}, warehouse=${measurement.warehouseId}]"
    }

    data class Normal(
        val measurement: SensorMeasurement
    ) : AlarmResult
}
