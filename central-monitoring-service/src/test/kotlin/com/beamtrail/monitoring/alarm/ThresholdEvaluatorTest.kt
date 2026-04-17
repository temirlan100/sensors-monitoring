package com.beamtrail.monitoring.alarm

import com.beamtrail.monitoring.model.SensorId
import com.beamtrail.monitoring.model.SensorMeasurement
import com.beamtrail.monitoring.model.SensorType
import com.beamtrail.monitoring.model.SensorValue
import com.beamtrail.monitoring.model.WarehouseId
import com.beamtrail.monitoring.config.ThresholdConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ThresholdEvaluatorTest {

    private val config = object : ThresholdConfig {
        override fun sensors(): Map<String, Double> = mapOf(
            "TEMPERATURE" to 35.0,
            "HUMIDITY" to 50.0
        )
    }
    private val evaluator = ThresholdEvaluator(config)

    private fun scalarMeasurement(typeName: String, amount: Double, unit: String? = null) = SensorMeasurement(
        sensorId = SensorId("sensor-1"),
        type = SensorType(typeName),
        value = SensorValue.Scalar(amount, unit),
        warehouseId = WarehouseId("warehouse-1")
    )

    @Test
    fun `should trigger alarm when temperature exceeds threshold`() {
        val result = evaluator.evaluate(scalarMeasurement("TEMPERATURE", 36.0, "°C"))

        assertThat(result).isInstanceOf(AlarmResult.Triggered::class.java)
        with(result as AlarmResult.Triggered) {
            assertThat((measurement.value as SensorValue.Scalar).amount).isEqualTo(36.0)
            assertThat(threshold).isEqualTo(35.0)
            assertThat(message).contains("ALARM", "36.0")
        }
    }

    @Test
    fun `should not trigger alarm when temperature is within threshold`() {
        val result = evaluator.evaluate(scalarMeasurement("TEMPERATURE", 30.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }

    @Test
    fun `should not trigger alarm when temperature equals threshold`() {
        val result = evaluator.evaluate(scalarMeasurement("TEMPERATURE", 35.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }

    @Test
    fun `should trigger alarm when humidity exceeds threshold`() {
        val result = evaluator.evaluate(scalarMeasurement("HUMIDITY", 55.0, "%"))

        assertThat(result).isInstanceOf(AlarmResult.Triggered::class.java)
        with(result as AlarmResult.Triggered) {
            assertThat(threshold).isEqualTo(50.0)
            assertThat(message).contains("ALARM", "HUMIDITY")
        }
    }

    @Test
    fun `should not trigger alarm when humidity is within threshold`() {
        val result = evaluator.evaluate(scalarMeasurement("HUMIDITY", 45.0))
        assertThat(result).isInstanceOf(AlarmResult.Normal::class.java)
    }

    @Test
    fun `should skip when no threshold configured for sensor type`() {
        val result = evaluator.evaluate(scalarMeasurement("CO2", 1200.0, "ppm"))

        assertThat(result).isInstanceOf(AlarmResult.Skipped::class.java)
        assertThat((result as AlarmResult.Skipped).reason).contains("No threshold configured")
    }

    @Test
    fun `should skip when value is non-scalar (location)`() {
        val measurement = SensorMeasurement(
            sensorId = SensorId("gps-1"),
            type = SensorType("LOCATION"),
            value = SensorValue.Location(51.5074, -0.1278),
            warehouseId = WarehouseId("warehouse-1")
        )
        val result = evaluator.evaluate(measurement)

        assertThat(result).isInstanceOf(AlarmResult.Skipped::class.java)
        assertThat((result as AlarmResult.Skipped).reason).contains("Non-scalar")
    }

    @Test
    fun `should skip when value is non-scalar (composite)`() {
        val measurement = SensorMeasurement(
            sensorId = SensorId("hvac-1"),
            type = SensorType("HVAC_STATUS"),
            value = SensorValue.Composite(
                mapOf(
                    "temperature" to SensorValue.Scalar(22.5),
                    "mode" to SensorValue.Textual("cooling")
                )
            ),
            warehouseId = WarehouseId("warehouse-1")
        )
        val result = evaluator.evaluate(measurement)

        assertThat(result).isInstanceOf(AlarmResult.Skipped::class.java)
        assertThat((result as AlarmResult.Skipped).reason).contains("Non-scalar")
    }
}
