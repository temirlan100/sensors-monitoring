package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorId
import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorType
import com.beamtrail.monitoring.model.WarehouseId
import com.beamtrail.monitoring.config.ThresholdConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ThresholdEvaluatorTest {

    private val config = object : ThresholdConfig {
        override fun temperature(): Double = 35.0
        override fun humidity(): Double = 50.0
    }
    private val evaluator = ThresholdEvaluator(config)

    private fun measurement(type: SensorType, value: Double) = SensorMeasurement(
        sensorId = SensorId("sensor-1"),
        type = type,
        value = value,
        warehouseId = WarehouseId("warehouse-1")
    )

    @Test
    fun `should trigger alarm when temperature exceeds threshold`() {
        val result = evaluator.evaluate(measurement(SensorType.TEMPERATURE, 36.0))

        assertThat(result).isInstanceOf(AlarmResult.Triggered::class.java)
        with(result as AlarmResult.Triggered) {
            assertThat(this.measurement.value).isEqualTo(36.0)
            assertThat(threshold).isEqualTo(35.0)
            assertThat(message).contains("ALARM", "36.0")
        }
    }

    @Test
    fun `should not trigger alarm when temperature is within threshold`() {
        val result = evaluator.evaluate(measurement(SensorType.TEMPERATURE, 30.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }

    @Test
    fun `should not trigger alarm when temperature equals threshold`() {
        val result = evaluator.evaluate(measurement(SensorType.TEMPERATURE, 35.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }

    @Test
    fun `should trigger alarm when humidity exceeds threshold`() {
        val result = evaluator.evaluate(measurement(SensorType.HUMIDITY, 55.0))

        assertThat(result).isInstanceOf(AlarmResult.Triggered::class.java)
        with(result as AlarmResult.Triggered) {
            assertThat(threshold).isEqualTo(50.0)
            assertThat(message).contains("ALARM", "HUMIDITY")
        }
    }

    @Test
    fun `should not trigger alarm when humidity is within threshold`() {
        val result = evaluator.evaluate(measurement(SensorType.HUMIDITY, 45.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }
}
