package com.beamtrail.monitoring.consumer

import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorValue
import com.beamtrail.monitoring.alarm.AlarmResult
import com.beamtrail.monitoring.alarm.ThresholdEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class MeasurementConsumer(
    private val evaluator: ThresholdEvaluator,
    private val objectMapper: ObjectMapper,
) {

    private val log = Logger.getLogger(MeasurementConsumer::class.java)

    @Incoming("sensor-measurements-in")
    suspend fun consume(raw: String) {
        val measurement = withContext(IO) {
            objectMapper.readValue(raw, SensorMeasurement::class.java)
        }

        log.info(
            "Received: sensor=${measurement.sensorId}, type=${measurement.type.name}, " +
                    "value=${formatValue(measurement.value)}, warehouse=${measurement.warehouseId}"
        )

        when (val result = evaluator.evaluate(measurement)) {
            is AlarmResult.Triggered -> log.error(result.message)
            is AlarmResult.Normal -> log.info(
                "Within range: sensor=${result.measurement.sensorId}, value=${formatValue(result.measurement.value)}"
            )
            is AlarmResult.Skipped -> log.debug(
                "Skipped: sensor=${result.measurement.sensorId}, reason=${result.reason}"
            )
        }
    }

    private fun formatValue(value: SensorValue): String = when (value) {
        is SensorValue.Scalar -> "${value.amount}${value.unit ?: ""}"
        is SensorValue.Location -> "(${value.latitude}, ${value.longitude})"
        is SensorValue.Textual -> value.content
        is SensorValue.Composite -> value.fields.entries.joinToString(", ") { "${it.key}=${formatValue(it.value)}" }
    }
}
