package com.beamtrail.warehouse.kafka

import com.beamtrail.warehouse.model.SensorMeasurement
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.future.await
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger

@ApplicationScoped
class MeasurementEmitter(
    @param:Channel("sensor-measurements-out")
    private val emitter: Emitter<String>,
    private val objectMapper: ObjectMapper
) {

    private val log = Logger.getLogger(MeasurementEmitter::class.java)

    suspend fun send(measurement: SensorMeasurement) {
        val json = objectMapper.writeValueAsString(measurement)
        log.info("Publishing: sensor=${measurement.sensorId}, type=${measurement.type}, value=${measurement.value}")
        emitter.send(json).toCompletableFuture().await()
    }
}
