package com.beamtrail.warehouse

import com.beamtrail.warehouse.kafka.MeasurementEmitter
import com.beamtrail.warehouse.model.SensorId
import com.beamtrail.warehouse.model.SensorMeasurement
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CompletableFuture
import org.mockito.kotlin.any

class MeasurementEmitterTest {

    private val objectMapper = jacksonObjectMapper()
    private val kafkaEmitter: Emitter<String> = mock()
    private val measurementEmitter = MeasurementEmitter(kafkaEmitter, objectMapper)

    @Test
    fun `should serialize measurement to JSON and send via emitter`() = runTest {
        val future = CompletableFuture.completedFuture(null as Void?)
        whenever(kafkaEmitter.send(any<String>())).thenReturn(future)

        val measurement = SensorMeasurement(
            sensorId = SensorId("t1"),
            type = SensorType.TEMPERATURE,
            value = 36.5,
            warehouseId = WarehouseId("warehouse-1")
        )

        measurementEmitter.send(measurement)

        val captor = argumentCaptor<String>()
        verify(kafkaEmitter).send(captor.capture())

        val json = captor.firstValue
        val deserialized = objectMapper.readValue(json, SensorMeasurement::class.java)
        assertThat(deserialized.sensorId).isEqualTo(SensorId("t1"))
        assertThat(deserialized.type).isEqualTo(SensorType.TEMPERATURE)
        assertThat(deserialized.value).isEqualTo(36.5)
        assertThat(deserialized.warehouseId).isEqualTo(WarehouseId("warehouse-1"))
    }

    @Test
    fun `should serialize humidity measurement correctly`() = runTest {
        val future = CompletableFuture.completedFuture(null as Void?)
        whenever(kafkaEmitter.send(any<String>())).thenReturn(future)

        val measurement = SensorMeasurement(
            sensorId = SensorId("h1"),
            type = SensorType.HUMIDITY,
            value = 55.0,
            warehouseId = WarehouseId("warehouse-2")
        )

        measurementEmitter.send(measurement)

        val captor = argumentCaptor<String>()
        verify(kafkaEmitter).send(captor.capture())

        val json = captor.firstValue
        assertThat(json).contains("\"sensorId\":\"h1\"")
        assertThat(json).contains("\"type\":\"HUMIDITY\"")
        assertThat(json).contains("\"value\":55.0")
    }
}
